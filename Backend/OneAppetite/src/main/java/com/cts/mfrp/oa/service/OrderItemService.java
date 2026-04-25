package com.cts.mfrp.oa.service;

import com.cts.mfrp.oa.dto.request.CartRequest;
import com.cts.mfrp.oa.dto.response.*;
import com.cts.mfrp.oa.exception.InvalidCartStateException;
import com.cts.mfrp.oa.exception.ResourceNotFoundException;
import com.cts.mfrp.oa.model.*;
import com.cts.mfrp.oa.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderItemService {

    @Autowired private OrderRepository orderRepo;
    @Autowired private OrderItemRepository itemRepo;
    @Autowired private MenuItemRepository menuRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private WalletService walletService;
    @Autowired private TokenService tokenService;

    @Transactional
    public OrderItemDTO addProductToCart(CartRequest request) {
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }

        MenuItem item = menuRepo.findForUpdate(request.menuItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found"));

        if (!item.getIsInStock() || item.getQuantityAvailable() <= 0) {
            throw new InvalidCartStateException("Sorry, " + item.getItemName() + " is currently out of stock.");
        }

        if (item.getQuantityAvailable() < request.quantity()) {
            throw new InvalidCartStateException("Insufficient stock.");
        }

        int remainingStock = item.getQuantityAvailable() - request.quantity();
        item.setQuantityAvailable(remainingStock);
        if (remainingStock == 0) {
            item.setIsInStock(false);
        }
        menuRepo.saveAndFlush(item);

        Order cart = orderRepo.findByUser_UserIdAndStatus(request.userId(), OrderStatus.CART)
                .orElseGet(() -> {
                    Order newOrder = new Order();
                    newOrder.setUser(userRepo.findById(request.userId()).orElseThrow());
                    newOrder.setVendor(item.getVendor());
                    newOrder.setStatus(OrderStatus.CART);
                    newOrder.setTotalAmount(0.0f);
                    newOrder.setOrderTime(LocalDateTime.now());
                    return orderRepo.save(newOrder);
                });

        if (!cart.getVendor().getUserId().equals(item.getVendor().getUserId())) {
            throw new InvalidCartStateException("You can only add items from " + cart.getVendor().getName() + " to this cart.");
        }

        OrderItem resultItem;
        OrderItem existing = itemRepo.findByOrder_OrderIdAndMenuItem_ItemId(
                cart.getOrderId(), request.menuItemId());

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

    public CartResponseDTO getActiveCart(Integer userId) {
        Order cart = orderRepo.findByUser_UserIdAndStatus(userId, OrderStatus.CART)
                .orElseThrow(() -> new ResourceNotFoundException("No active cart found for user ID: " + userId));

        return mapToCartDTO(cart);
    }

    public List<CartResponseDTO> getOrderHistory(Integer userId) {
        return orderRepo.findByUser_UserIdAndStatusNotOrderByOrderTimeDesc(userId, OrderStatus.CART)
                .stream().map(this::mapToCartDTO).toList();
    }

    @Transactional
    public CartResponseDTO placeOrder(Integer userId) {
        Order cart = orderRepo.findByUser_UserIdAndStatus(userId, OrderStatus.CART)
                .orElseThrow(() -> new ResourceNotFoundException("No active cart found for user ID: " + userId));

        Float total = cart.getTotalAmount();
        if (total == null || total <= 0f) {
            throw new InvalidCartStateException("Cart is empty.");
        }

        walletService.debit(userId, total.doubleValue());

        if (cart.getTokenNumber() == null) {
            cart.setTokenNumber(tokenService.generateUniqueToken());
        }
        cart.setStatus(OrderStatus.PLACED);
        cart.setOrderTime(LocalDateTime.now());
        return mapToCartDTO(orderRepo.save(cart));
    }

    private CartResponseDTO mapToCartDTO(Order order) {
        User v = order.getVendor();
        VendorDTO vendorDTO = new VendorDTO(
                v.getUserId(), v.getName(), v.getEmail(),
                v.getPhone(), v.getIsActive(), v.getVendorName(), v.getVendorType() == null ? null : v.getVendorType().name()
        );

        List<OrderItemDTO> itemDTOs = order.getOrderItems().stream()
                .map(this::mapToDTO)
                .toList();

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
                v.getPhone(), v.getIsActive(), v.getVendorName(), v.getVendorType() == null ? null : v.getVendorType().name()
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

    @Transactional
    public void reduceQuantity(Integer orderItemId) {
        OrderItem item = itemRepo.findById(orderItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in cart"));

        Order order = item.getOrder();
        MenuItem menuItem = menuRepo.findForUpdate(item.getMenuItem().getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));

        if (item.getQuantity() > 1) {
            menuItem.setQuantityAvailable(menuItem.getQuantityAvailable() + 1);
            menuItem.setIsInStock(true);
            menuRepo.saveAndFlush(menuItem);

            item.setQuantity(item.getQuantity() - 1);
            item.setPrice((float) (menuItem.getPrice() * item.getQuantity()));
            itemRepo.save(item);
            itemRepo.flush();
            updateOrderAggregates(order);
        } else {
            menuItem.setQuantityAvailable(menuItem.getQuantityAvailable() + item.getQuantity());
            menuItem.setIsInStock(true);
            menuRepo.saveAndFlush(menuItem);

            itemRepo.delete(item);
            itemRepo.flush();

            long remaining = itemRepo.findByOrder_OrderId(order.getOrderId()).size();
            if (remaining == 0 && order.getStatus() == OrderStatus.CART) {
                orderRepo.delete(order);
                orderRepo.flush();
            } else {
                updateOrderAggregates(order);
            }
        }
    }

    public Float getCartTotal(Integer userId) {
        return orderRepo.findByUser_UserIdAndStatus(userId, OrderStatus.CART)
                .map(Order::getTotalAmount)
                .orElse(0.0f);
    }
}
