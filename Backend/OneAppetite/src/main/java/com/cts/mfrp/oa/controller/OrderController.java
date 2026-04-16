package com.cts.mfrp.oa.controller;

import com.cts.mfrp.oa.model.Order;
import com.cts.mfrp.oa.model.OrderStatus;
import com.cts.mfrp.oa.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {
<<<<<<< HEAD
    @Autowired
    private OrderService orderService;

    @PutMapping("/{orderId}/status")
    public ResponseEntity<Order> updateStatus(@PathVariable Integer orderId,
                                              @RequestParam OrderStatus status) {
        Order updatedOrder = orderService.updateOrderStatus(orderId, status);

        // Simple notification (log for now)
        System.out.println("Employee notified: Order " + orderId + " moved to " + status);

=======

    @Autowired
    private OrderService orderService;

    @PutMapping("/{id}/status")
    public ResponseEntity<Order> updateStatus(@PathVariable Long id,
                                              @RequestParam OrderStatus status) {
        Order updatedOrder = orderService.updateOrderStatus(id, status);
>>>>>>> 51ca6366f48c201655e89ee6b836107a5c8aeb83
        return ResponseEntity.ok(updatedOrder);
    }
}
