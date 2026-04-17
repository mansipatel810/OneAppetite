
package com.cts.mfrp.oa.events;

import com.cts.mfrp.oa.model.Order;
import org.springframework.context.ApplicationEvent;

public class OrderReadyEvent extends ApplicationEvent {
    private final Order order;

    // ✅ Constructor must accept (Object source, Order order)
    public OrderReadyEvent(Object source, Order order) {
        super(source);   // pass source to ApplicationEvent
        this.order = order;
    }

    public Order getOrder() {
        return order;
    }
}
