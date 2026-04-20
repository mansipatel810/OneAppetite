package com.cts.mfrp.oa.events;

import com.cts.mfrp.oa.model.Order;
import com.cts.mfrp.oa.model.OrderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {

    @Autowired
    private ApplicationEventPublisher publisher;

    public void publishOrderPlaced(Order order) {
        if (order.getStatus() == OrderStatus.PLACED) {
            publisher.publishEvent(new OrderPlacedEvent(this, order));
        }
        if (order.getStatus() == OrderStatus.READY) {
            publisher.publishEvent(new OrderReadyEvent(this, order));
        }
    }
}
