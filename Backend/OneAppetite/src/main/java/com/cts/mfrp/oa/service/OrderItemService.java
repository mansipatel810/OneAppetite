package com.cts.mfrp.oa.service;

import com.cts.mfrp.oa.dto.request.CartRequest;
import com.cts.mfrp.oa.model.*;
import com.cts.mfrp.oa.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class OrderItemService {

    @Autowired private OrderRepository orderRepo;
    @Autowired private OrderItemRepository itemRepo;
    @Autowired private MenuItemRepository menuRepo;
    @Autowired private UserRepository userRepo;

    @Transactional
    public OrderItem addProductToCart(CartRequest request) {
        // 1. Validate Item and Stock
        MenuItem item = menuRepo.findById(request.menuItemId())
                .orElseThrow(() -> new RuntimeException("Item not found"));

        if (!item.getIsInStock() || item.getQuantityAvailable() <= 0) {
            throw new RuntimeException("Sorry, " + item.getItemName() + " is currently out of stock.");
        }

        if (item.getQuantityAvailable() < request.quantity()) {
            throw new RuntimeException("Insufficient stock.");
        }

        // --- STOCK REDUCTION ---
        int remainingStock = item.getQuantityAvailable() - request.quantity();
        item.setQuantityAvailable(remainingStock);
        if (remainingStock == 0) {
            item.setIsInStock(false);
        }
        menuRepo.saveAndFlush(item);

        // 2. Find or Create Cart
        Order cart = orderRepo.findByUser_UserIdAndStatus(request.userId(), "CART")
                .orElseGet(() -> {
                    Order newOrder = new Order();
                    newOrder.setUser(userRepo.findById(request.userId()).orElseThrow());

                    // FIX: Set the Vendor ID from the MenuItem
                    newOrder.setVendor(item.getVendor());
                    newOrder.setStatus(OrderStatus.CART);
                    newOrder.setTotalAmount(0.0f);
                    newOrder.setOrderTime(LocalDateTime.now());
                    return orderRepo.save(newOrder);
                });

        // OPTIONAL: Logic check to prevent adding items from different vendors to the same cart
        if (!cart.getVendor().getUserId().equals(item.getVendor().getUserId())) {
            throw new RuntimeException("You can only add items from " + cart.getVendor().getName() + " to this cart.");
        }

        OrderItem resultItem;

        // 3. Update or Create OrderItem
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

        // 4. Update Aggregates
        updateOrderAggregates(cart);

        return resultItem;
    }

    private void updateOrderAggregates(Order order) {
        // Fetch fresh list from DB to calculate total correctly
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
        if (maxPrepTime > 0) {
            order.setReadyTime(LocalDateTime.now().plusMinutes(maxPrepTime));
        } else {
            order.setReadyTime(LocalDateTime.now().plusMinutes(15));
        }

        orderRepo.save(order);
    }

    @Transactional
    public void reduceQuantity(Integer orderItemId) {
        // 1. Find the Item in the cart
        OrderItem item = itemRepo.findById(orderItemId)
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));

        Order order = item.getOrder();
        MenuItem menuItem = item.getMenuItem();

        // 2. Logic: Reduce by 1 and return 1 unit to stock
        int currentQuantity = item.getQuantity();

        // Always return 1 unit to the menu stock
        menuItem.setQuantityAvailable(menuItem.getQuantityAvailable() + 1);
        if (!menuItem.getIsInStock()) {
            menuItem.setIsInStock(true);
        }
        menuRepo.saveAndFlush(menuItem);

        if (currentQuantity > 1) {
            // Just decrease the number
            item.setQuantity(currentQuantity - 1);
            // Recalculate price for this line item (Price * new quantity)
            item.setPrice((float) (menuItem.getPrice() * item.getQuantity()));
            itemRepo.save(item);
        } else {
            // If it was the last 1, remove the row entirely
            itemRepo.delete(item);
        }

        // Force a flush so the update/delete is finished before we calculate the order total
        itemRepo.flush();

        // 3. Recalculate the Order total and ready time
        updateOrderAggregates(order);
    }

    public Float getCartTotal(Integer userId) {
        return orderRepo.findByUser_UserIdAndStatus(userId, "CART")
                .map(Order::getTotalAmount)
                .orElse(0.0f);
    }

    // Alternatively, if you want the full Cart details including items
    public Order getActiveCart(Integer userId) {
        return orderRepo.findByUser_UserIdAndStatus(userId, "CART")
                .orElseThrow(() -> new RuntimeException("No active cart found for user ID: " + userId));
    }
}