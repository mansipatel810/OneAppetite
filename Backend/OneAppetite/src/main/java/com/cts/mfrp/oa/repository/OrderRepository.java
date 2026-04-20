package com.cts.mfrp.oa.repository;

import com.cts.mfrp.oa.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    Optional<Order> findByUser_UserIdAndStatus(Integer userId, String status);

    List<Order> findByUser_UserIdAndStatusNotOrderByOrderTimeDesc(Integer userId, String status);
}