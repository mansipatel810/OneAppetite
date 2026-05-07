package com.cts.mfrp.oa.service;

import com.cts.mfrp.oa.dto.request.CartRequest;
import com.cts.mfrp.oa.dto.response.*;
import com.cts.mfrp.oa.exception.OutOfStockException;
import com.cts.mfrp.oa.exception.ResourceNotFoundException;
import com.cts.mfrp.oa.model.*;
import com.cts.mfrp.oa.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Cart and order placement.
 *
 * <p>The cart model is multi-vendor: a user may hold one CART order per vendor
 * simultaneously. {@code addProductToCart} finds-or-creates the cart specific
 * to the item's vendor, so adding a Sushi Zen item never collides with a
 * Madurai Mornings cart.
 *
 * <p>{@code placeAllCarts} iterates every CART for the user, transitions each
 * to PLACED with its own token, debits the customer's wallet for the grand
 * total, and credits each vendor with only their own share.
 */
@Service
public class OrderItemService {

    @Autowired private OrderRepository orderRepo;
    @Autowired private OrderItemRepository itemRepo;
    @Autowired private MenuItemRepository menuRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private WalletService walletService;
    @Autowired private NotificationService notificationService;

    private static final Random TOKEN_RNG = new Random();

    /* ── Add to cart (multi-vendor aware) ────────────────────── */
    @Transactional
    public OrderItemDTO addProductToCart(CartRequest request) {
        MenuItem item = menuRepo.findById(request.menuItemId())
                .orElseThrow(() -> new RuntimeException("Item not found"));

        // Soft availability check at add-time. Stock is NOT decremented here —
        // the database row stays at its current quantity until the customer
        // actually clicks Pay. The authoritative re-check + decrement runs
        // inside placeAllCarts() under a pessimistic row lock, which is what
        // serializes the "two users buy the last item" race.
        if (!item.getIsInStock() || item.getQuantityAvailable() <= 0) {
            throw new OutOfStockException("Sorry, " + item.getItemName() + " is currently out of stock.");
        }

        // Find or create the cart specific to (user, this item's vendor).
        // No more single-vendor restriction — the customer can hold one cart
        // bucket per vendor at the same time.
        final Integer userId   = request.userId();
        final Integer vendorId = item.getVendor().getUserId();

        Order cart = orderRepo
                .findByUser_UserIdAndVendor_UserIdAndStatus(userId, vendorId, OrderStatus.CART)
                .orElseGet(() -> {
                    Order newOrder = new Order();
                    newOrder.setUser(userRepo.findById(userId).orElseThrow());
                    newOrder.setVendor(item.getVendor());
                    newOrder.setStatus(OrderStatus.CART);
                    newOrder.setTotalAmount(0.0f);
                    newOrder.setOrderTime(LocalDateTime.now());
                    return orderRepo.save(newOrder);
                });

        OrderItem resultItem;
        OrderItem existing = itemRepo.findByOrder_OrderIdAndMenuItem_ItemId(
                cart.getOrderId(), request.menuItemId());

        // Compare against (already-in-cart + this-add) so a customer can't
        // keep clicking + on a 1-stock item until their cart row says 5.
        // Stock is reserved at PAY time, not here — but we still want to stop
        // clearly impossible adds at the source.
        int alreadyInCart = existing == null ? 0 : existing.getQuantity();
        if (item.getQuantityAvailable() < (alreadyInCart + request.quantity())) {
            throw new OutOfStockException(
                    "Only " + item.getQuantityAvailable() + " of " + item.getItemName()
                  + " left — your cart already holds " + alreadyInCart + ".");
        }

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + request.quantity());
            existing.setPrice((float) (item.getPrice() * existing.getQuantity()));
            resultItem = itemRepo.save(existing);
        } else {
            OrderItem newItem = new OrderItem();
            newItem.setOrder(cart);
            newItem.setMenuItem(item);
            newItem.setQuantity(request.quantity());
            newItem.setPrice((float) (item.getPrice() * request.quantity()));
            resultItem = itemRepo.save(newItem);
        }

        updateOrderAggregates(cart);
        return mapToDTO(resultItem);
    }

    /* ── View cart ───────────────────────────────────────────── */

    /**
     * Legacy single-bucket view. Returns the first cart for the user so older
     * single-vendor frontends keep working. Prefer {@link #getActiveCarts}.
     */
    public CartResponseDTO getActiveCart(Integer userId) {
        List<Order> carts = orderRepo.findAllByUser_UserIdAndStatus(userId, OrderStatus.CART);
        if (carts.isEmpty()) {
            throw new RuntimeException("No active cart found for user ID: " + userId);
        }
        return mapToCartDTO(carts.get(0));
    }

    /** Multi-vendor view — one CartResponseDTO per vendor bucket. */
    public List<CartResponseDTO> getActiveCarts(Integer userId) {
        return orderRepo.findAllByUser_UserIdAndStatus(userId, OrderStatus.CART)
                .stream()
                .map(this::mapToCartDTO)
                .toList();
    }

    public List<CartResponseDTO> getOrderHistory(Integer userId) {
        return orderRepo.findByUser_UserIdAndStatusNotOrderByOrderTimeDesc(userId, OrderStatus.CART)
                .stream().map(this::mapToCartDTO).toList();
    }

    /* ── Place order (multi-vendor split) ────────────────────── */

    /**
     * Backwards-compat wrapper for legacy callers. Returns the first
     * finalized order so older frontends still get a usable response.
     */
    @Transactional
    public CartResponseDTO placeOrder(Integer userId) {
        List<CartResponseDTO> placed = placeAllCarts(userId);
        if (placed.isEmpty()) {
            throw new RuntimeException("No active cart found for user ID: " + userId);
        }
        return placed.get(0);
    }

    /**
     * Final placement of every cart bucket the user holds.
     *
     * <p>Order of operations matters — we want to be exactly fair to whoever
     * clicks Pay first, and never leave a customer charged for an item they
     * couldn't get:
     * <ol>
     *   <li><b>Lock</b> every menu_items row referenced by every bucket using
     *       {@link MenuItemRepository#findByIdForUpdate}. Pessimistic write,
     *       sorted by item id to avoid deadlocks if two transactions touch
     *       overlapping items.</li>
     *   <li><b>Verify</b> each item's current quantityAvailable still covers
     *       what's in the cart. If anything is short, throw
     *       {@link OutOfStockException} — the @Transactional rollback
     *       UN-DOES the wallet debit (because we haven't done it yet).</li>
     *   <li><b>Decrement</b> the locked rows; flip isInStock=false on any
     *       that hit zero; fire a vendor notification when stock crosses
     *       the low-stock threshold (1..4 remaining).</li>
     *   <li><b>Debit</b> the customer's wallet for the grand total — only
     *       now that we know every item is reserved.</li>
     *   <li><b>Transition</b> each cart to PLACED with its own token, and
     *       credit each vendor with their own share.</li>
     * </ol>
     *
     * <p>Pessimistic locking handles the common race; the {@code @Version}
     * column on MenuItem is a backstop for any code path that bypasses
     * the lock (e.g. the vendor stock-toggle endpoint).
     */
    @Transactional
    public List<CartResponseDTO> placeAllCarts(Integer userId) {
        List<Order> carts = orderRepo.findAllByUser_UserIdAndStatus(userId, OrderStatus.CART);
        if (carts.isEmpty()) {
            throw new RuntimeException("No active cart found for user ID: " + userId);
        }

        // Total qty needed per menu item, summed across vendor buckets (a
        // single item only ever lives in one bucket — its own vendor's —
        // but we sum defensively).
        Map<Integer, Integer> requestedByItemId = new HashMap<>();
        for (Order cart : carts) {
            if (cart.getOrderItems() == null) continue;
            for (OrderItem oi : cart.getOrderItems()) {
                requestedByItemId.merge(
                        oi.getMenuItem().getItemId(), oi.getQuantity(), Integer::sum);
            }
        }
        if (requestedByItemId.isEmpty()) {
            throw new RuntimeException("Cart is empty.");
        }

        // Step 1+2+3: lock + verify + decrement, in id order.
        // ──────────────────────────────────────────────────────────
        List<Integer> lockOrder = new ArrayList<>(requestedByItemId.keySet());
        java.util.Collections.sort(lockOrder);
        for (Integer itemId : lockOrder) {
            int requested = requestedByItemId.get(itemId);

            MenuItem locked = menuRepo.findByIdForUpdate(itemId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Menu item " + itemId + " not found"));

            int beforeQty = locked.getQuantityAvailable() == null
                    ? 0 : locked.getQuantityAvailable();

            if (!Boolean.TRUE.equals(locked.getIsInStock()) || beforeQty < requested) {
                // Stock disappeared between cart-add and pay. The wallet has
                // not been debited yet, so simply throwing rolls everything
                // back — no refund handling needed.
                throw new OutOfStockException(
                        "Sorry, " + locked.getItemName() + " just went out of stock!");
            }

            int afterQty = beforeQty - requested;
            locked.setQuantityAvailable(afterQty);
            if (afterQty == 0) {
                locked.setIsInStock(false);
            }
            menuRepo.save(locked);

            // Vendor low-stock alert. Trigger on the transition INTO the
            // 1..4 band (i.e. before was >=5, now <5). Hits zero trigger
            // their own out-of-stock alert.
            if (locked.getVendor() != null) {
                Integer vendorId = locked.getVendor().getUserId();
                if (afterQty == 0) {
                    notificationService.push(vendorId,
                            "❌ " + locked.getItemName() + " is now Out of Stock");
                } else if (beforeQty >= 5 && afterQty < 5) {
                    notificationService.push(vendorId,
                            "⚠ Low stock — only " + afterQty + " of "
                          + locked.getItemName() + " left. Restock soon.");
                }
            }
        }

        // Step 4: charge the customer ONCE for the grand total.
        // ──────────────────────────────────────────────────────────
        double grandTotal = 0.0;
        for (Order c : carts) {
            float t = c.getTotalAmount() == null ? 0f : c.getTotalAmount();
            if (t > 0f) grandTotal += t;
        }
        if (grandTotal <= 0.0) {
            throw new RuntimeException("Cart is empty.");
        }
        walletService.debit(userId, grandTotal);

        // Step 5: transition each bucket to PLACED + credit vendors.
        // ──────────────────────────────────────────────────────────
        List<CartResponseDTO> results = new ArrayList<>(carts.size());
        for (Order cart : carts) {
            float total = cart.getTotalAmount() == null ? 0f : cart.getTotalAmount();
            if (total <= 0f) continue;

            cart.setTokenNumber(generateUniqueToken());
            cart.setStatus(OrderStatus.PLACED);
            cart.setOrderTime(LocalDateTime.now());
            Order saved = orderRepo.save(cart);

            // Credit the vendor's earnings wallet. Failures are swallowed so
            // they don't undo the customer's already-debited transaction.
            if (saved.getVendor() != null) {
                try {
                    walletService.creditVendor(saved.getVendor().getUserId(), total);
                } catch (Exception e) {
                    System.err.println("[OrderItemService] vendor credit failed for vendor "
                            + saved.getVendor().getUserId() + ": " + e.getMessage());
                }
            }

            // Notify both sides of this specific order.
            String label = saved.getTokenNumber();
            notificationService.push(userId, "Order " + label + " placed successfully");
            if (saved.getVendor() != null) {
                notificationService.push(saved.getVendor().getUserId(),
                        "New order " + label + " received");
            }
            results.add(mapToCartDTO(saved));
        }
        return results;
    }

    /* ── Mapping helpers ─────────────────────────────────────── */

    private CartResponseDTO mapToCartDTO(Order order) {
        User v = order.getVendor();
        VendorDTO vendorDTO = v == null ? null : new VendorDTO(
                v.getUserId(), v.getName(), v.getEmail(),
                v.getPhone(), v.getIsActive(), v.getVendorName(), v.getVendorType()
        );

        List<OrderItemDTO> itemDTOs = order.getOrderItems() == null
                ? List.of()
                : order.getOrderItems().stream().map(this::mapToDTO).toList();

        return new CartResponseDTO(
                order.getOrderId(),
                order.getTokenNumber(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getOrderTime(),
                order.getReadyTime(),
                vendorDTO,
                itemDTOs
        );
    }

    private OrderItemDTO mapToDTO(OrderItem entity) {
        User v = entity.getMenuItem().getVendor();
        VendorDTO vendorDTO = new VendorDTO(
                v.getUserId(), v.getName(), v.getEmail(),
                v.getPhone(), v.getIsActive(), v.getVendorName(), v.getVendorType()
        );

        MenuItem m = entity.getMenuItem();
        MenuItemDTO menuDTO = new MenuItemDTO(
                m.getItemId(), m.getItemName(), m.getCategory(),
                m.getPrice(), m.getQuantityAvailable(), m.getIsInStock(), vendorDTO
        );

        return new OrderItemDTO(
                entity.getOrderItemId(), entity.getQuantity(),
                entity.getPrice(), menuDTO
        );
    }

    private void updateOrderAggregates(Order order) {
        List<OrderItem> items = itemRepo.findByOrder_OrderId(order.getOrderId());
        float newTotal = 0.0f;
        int maxPrepTime = 0;

        for (OrderItem item : items) {
            newTotal += item.getPrice();
            Integer itemPrepTime = item.getMenuItem().getMinPrepTime();
            if (itemPrepTime != null && itemPrepTime > maxPrepTime) {
                maxPrepTime = itemPrepTime;
            }
        }

        order.setTotalAmount(newTotal);
        order.setReadyTime(LocalDateTime.now().plusMinutes(maxPrepTime > 0 ? maxPrepTime : 15));
        orderRepo.save(order);
    }

    /**
     * Reduces or removes an item from the cart. Stock is NOT touched here —
     * since adds no longer reserve stock, removes have nothing to release.
     */
    @Transactional
    public void reduceQuantity(Integer orderItemId) {
        OrderItem item = itemRepo.findById(orderItemId)
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));

        Order order = item.getOrder();
        MenuItem menuItem = item.getMenuItem();

        boolean orderItemDeleted = false;
        if (item.getQuantity() > 1) {
            item.setQuantity(item.getQuantity() - 1);
            item.setPrice((float) (menuItem.getPrice() * item.getQuantity()));
            itemRepo.save(item);
        } else {
            itemRepo.delete(item);
            orderItemDeleted = true;
        }
        itemRepo.flush();

        // If the bucket is empty after this reduce, drop the CART order so it
        // doesn't leave a ghost row that placeOrder would later try to settle.
        if (orderItemDeleted
                && order.getStatus() == OrderStatus.CART
                && itemRepo.findByOrder_OrderId(order.getOrderId()).isEmpty()) {
            orderRepo.delete(order);
        } else {
            updateOrderAggregates(order);
        }
    }

    /** Sums every cart bucket the user has open. */
    public Float getCartTotal(Integer userId) {
        return (float) orderRepo.findAllByUser_UserIdAndStatus(userId, OrderStatus.CART)
                .stream()
                .mapToDouble(o -> o.getTotalAmount() == null ? 0.0 : o.getTotalAmount())
                .sum();
    }

    /* ── Token generation ────────────────────────────────────── */
    private String generateUniqueToken() {
        String token;
        int attempts = 0;
        do {
            token = "OA-" + (1000 + TOKEN_RNG.nextInt(9000));
            if (++attempts > 50) {
                // Extremely unlikely with a 9000-value space, but defensive.
                return "OA-" + (1000 + TOKEN_RNG.nextInt(9000)) + "-" + (System.currentTimeMillis() % 100);
            }
        } while (orderRepo.existsByTokenNumber(token));
        return token;
    }
}
