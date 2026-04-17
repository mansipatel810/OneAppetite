package com.cts.mfrp.oa.listener;

import com.cts.mfrp.oa.util.Mp3Player;
import com.cts.mfrp.oa.events.OrderReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class OrderReadyListener {

    @EventListener
    public void handleOrderReady(OrderReadyEvent event) {
        System.out.println("Employee notified: Order " + event.getOrder().getOrderId() + " is READY");
        Mp3Player.play("orderReady.mp3");  // plays from resources
    }
}

