package com.cts.mfrp.oa.repository;

import com.cts.mfrp.oa.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    OrderItem findByOrder_OrderIdAndMenuItem_ItemId(Integer orderId, Integer itemId);

    // Add this line to fetch all items for an order
    List<OrderItem> findByOrder_OrderId(Integer orderId);

    boolean existsByMenuItem_ItemId(Integer itemId);
}