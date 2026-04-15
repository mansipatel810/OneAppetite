package com.cts.mfrp.oa.repository;

import com.cts.mfrp.oa.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
