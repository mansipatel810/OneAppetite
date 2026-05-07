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
    @Autowired
    private NotificationService notificationService;


    public Order updateOrderStatus(Integer orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(newStatus);
        Order savedOrder = orderRepository.save(order);
        orderEventPublisher.publishOrderPlaced(savedOrder);

        // Notify customer about status transition
        if (savedOrder.getUser() != null) {
            String token = savedOrder.getTokenNumber() != null ? savedOrder.getTokenNumber() : ("#" + savedOrder.getOrderId());
            String message = switch (newStatus) {
                case PREPARING -> "Order " + token + " is now being prepared";
                case READY     -> "Order " + token + " is ready for pickup!";
                case PICKED_UP -> "Order " + token + " — picked up. Enjoy your meal!";
                case COMPLETED -> "Order " + token + " has been completed";
                default        -> "Order " + token + " status: " + newStatus.name();
            };
            notificationService.push(savedOrder.getUser().getUserId(), message);
        }
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
        Order saved = orderRepository.save(cart);

        // Notify customer + vendor about the new order
        notificationService.push(userId, "Order " + saved.getTokenNumber() + " placed successfully");
        if (saved.getVendor() != null) {
            notificationService.push(saved.getVendor().getUserId(),
                    "New order " + saved.getTokenNumber() + " received");
        }
        return saved;
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
