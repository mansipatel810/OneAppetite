package com.cts.mfrp.oa.listener;

import com.cts.mfrp.oa.util.Mp3Player;
import com.cts.mfrp.oa.events.OrderPlacedEvent;
import com.cts.mfrp.oa.model.Order;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class OrderPlacedListener {

    @EventListener
    public void handleOrderPlaced(OrderPlacedEvent event) {
        // ✅ Get the Order object from the event
        Order order = event.getOrder();

        // ✅ Use the Order's methods
        System.out.println("Employee notified: Order "
                + order.getOrderId()
                + " moved to " + order.getStatus());

        Mp3Player.play("orderPlaced.mp3");  // plays from resources
    }
}


