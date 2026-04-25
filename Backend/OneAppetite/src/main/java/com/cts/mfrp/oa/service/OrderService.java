package com.cts.mfrp.oa.service;

import com.cts.mfrp.oa.exception.ResourceNotFoundException;
import com.cts.mfrp.oa.model.Order;
import com.cts.mfrp.oa.model.OrderStatus;
import com.cts.mfrp.oa.repository.OrderRepository;
import com.cts.mfrp.oa.events.OrderEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.List;

@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderEventPublisher orderEventPublisher;


    public Order updateOrderStatus(Integer orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        order.setStatus(newStatus);
        Order savedOrder = orderRepository.save(order);
        orderEventPublisher.publishOrderPlaced(savedOrder);
        return savedOrder;
    }

    public List<Order> getOrdersByVendor(Integer vendorId) {
        return orderRepository.findByVendor_UserIdAndStatusNot(vendorId, OrderStatus.CART);
    }

    @Transactional
    public Order placeOrder(Integer userId){
        Order cart = orderRepository.findByUser_UserIdAndStatus(userId, OrderStatus.CART)
                .orElseThrow(()-> new ResourceNotFoundException("No active cart found for user: " + userId));
        cart.setTokenNumber(generateUniqueToken());
        cart.setStatus(OrderStatus.PLACED);

        return orderRepository.save(cart);
    }

    private String generateUniqueToken(){
        Random random = new Random();
        String token;
        do{
            token = "OA-"+(1000+random.nextInt(9000));
        }while(orderRepository.existsByTokenNumber(token));
        return token;
    }
}