package com.cts.mfrp.oa.service;

import com.cts.mfrp.oa.model.Order;
import com.cts.mfrp.oa.model.OrderStatus;
import com.cts.mfrp.oa.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
<<<<<<< HEAD
    @Autowired
    private OrderRepository orderRepository;

    public Order updateOrderStatus(Integer orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(newStatus);
        return orderRepository.save(order);
=======

    @Autowired
    private OrderRepository orderRepository;

    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(newStatus);
        Order savedOrder = orderRepository.save(order);

        // Simple notification for now
        System.out.println("Employee notified: Order " + savedOrder.getId() +
                " is now " + savedOrder.getStatus());

        return savedOrder;
>>>>>>> 51ca6366f48c201655e89ee6b836107a5c8aeb83
    }
}
