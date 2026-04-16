package com.cts.mfrp.oa.repository;

import com.cts.mfrp.oa.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
<<<<<<< HEAD
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    Optional<Order> findByUser_UserIdAndStatus(Integer userId, String status);
}
=======

public interface OrderRepository extends JpaRepository<Order, Long> {
}
>>>>>>> 51ca6366f48c201655e89ee6b836107a5c8aeb83
