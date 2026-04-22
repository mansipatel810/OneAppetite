package com.cts.mfrp.oa.repository;

import com.cts.mfrp.oa.model.Order;
import com.cts.mfrp.oa.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    Optional<Order> findByUser_UserIdAndStatus(Integer userId, OrderStatus status);

    List<Order> findByUser_UserIdAndStatusNotOrderByOrderTimeDesc(Integer userId, OrderStatus status);

    boolean existsByTokenNumber(String tokenNumber);
    List<Order> findByVendor_UserIdAndStatusNot(Integer vendorId, OrderStatus status);
}
