package com.cts.mfrp.oa.repository;

import com.cts.mfrp.oa.model.Order;
import com.cts.mfrp.oa.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    /**
     * Legacy single-cart lookup. Kept for back-compat — returns the first
     * matching CART. With multi-vendor carts use {@link #findAllByUser_UserIdAndStatus}.
     */
    Optional<Order> findByUser_UserIdAndStatus(Integer userId, OrderStatus status);

    /** All orders for the user with the given status (multi-vendor carts). */
    List<Order> findAllByUser_UserIdAndStatus(Integer userId, OrderStatus status);

    /**
     * The single CART for (user, vendor). The multi-vendor cart logic enforces
     * exactly one such row per pair: each vendor gets its own cart bucket.
     */
    Optional<Order> findByUser_UserIdAndVendor_UserIdAndStatus(
            Integer userId, Integer vendorId, OrderStatus status);

    List<Order> findByUser_UserIdAndStatusNotOrderByOrderTimeDesc(Integer userId, OrderStatus status);

    boolean existsByTokenNumber(String tokenNumber);

    List<Order> findByVendor_UserIdAndStatusNot(Integer vendorId, OrderStatus status);
}
