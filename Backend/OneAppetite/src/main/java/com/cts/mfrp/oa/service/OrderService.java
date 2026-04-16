package com.cts.mfrp.oa.service;

import com.cts.mfrp.oa.exception.ResourceNotFoundException;
import com.cts.mfrp.oa.model.Order;
import com.cts.mfrp.oa.model.OrderStatus;
import com.cts.mfrp.oa.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;

    public Order updateOrderStatus(Integer orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(newStatus);
        return orderRepository.save(order);
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
