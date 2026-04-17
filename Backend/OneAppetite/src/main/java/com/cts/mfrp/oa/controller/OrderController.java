package com.cts.mfrp.oa.controller;

import com.cts.mfrp.oa.dto.response.*;
import com.cts.mfrp.oa.model.Order;
import com.cts.mfrp.oa.model.OrderItem;
import com.cts.mfrp.oa.model.MenuItem;
import com.cts.mfrp.oa.model.OrderStatus;
import com.cts.mfrp.oa.model.User;
import com.cts.mfrp.oa.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderDTO> updateStatus(@PathVariable Integer orderId,
                                                 @RequestParam OrderStatus status) {
        Order updatedOrder = orderService.updateOrderStatus(orderId, status);

        OrderDTO dto = convertToDTO(updatedOrder);
        System.out.println("Employee notified: Order " + orderId + " moved to " + status);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/place/{userId}")
    public ResponseEntity<OrderDTO> placeOrder(@PathVariable Integer userId) {
        Order finalizedOrder = orderService.placeOrder(userId);

        OrderDTO dto = convertToDTO(finalizedOrder);
        return ResponseEntity.ok(dto);
    }

    // Inline conversion method
    private OrderDTO convertToDTO(Order order) {
        User user = order.getUser();
        UserResponse userResponse = new UserResponse(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name(),
                user.getIsActive()
        );

        VendorDTO vendorDTO = new VendorDTO(
                order.getVendor().getUserId(),
                order.getVendor().getName(),
                order.getVendor().getEmail(),
                order.getVendor().getPhone(),
                order.getVendor().getIsActive(),
                order.getVendor().getVendorName()
        );

        return new OrderDTO(
                order.getOrderId(),
                order.getTokenNumber(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getOrderTime(),
                order.getReadyTime(),
                userResponse,
                vendorDTO,
                order.getOrderItems().stream().map(this::convertOrderItemToDTO).collect(Collectors.toList())
        );
    }

    private OrderItemDTO convertOrderItemToDTO(OrderItem item) {
        MenuItem menuItem = item.getMenuItem();
        VendorDTO vendorDTO = new VendorDTO(
                menuItem.getVendor().getUserId(),
                menuItem.getVendor().getName(),
                menuItem.getVendor().getEmail(),
                menuItem.getVendor().getPhone(),
                menuItem.getVendor().getIsActive(),
                menuItem.getVendor().getVendorName()
        );

        MenuItemDTO menuItemDTO = new MenuItemDTO(
                menuItem.getItemId(),
                menuItem.getItemName(),
                menuItem.getCategory(),
                menuItem.getPrice(),
                menuItem.getQuantityAvailable(),
                menuItem.getIsInStock(),
                vendorDTO
        );

        return new OrderItemDTO(
                item.getOrderItemId(),
                item.getQuantity(),
                item.getPrice(),
                menuItemDTO
        );
    }
}
