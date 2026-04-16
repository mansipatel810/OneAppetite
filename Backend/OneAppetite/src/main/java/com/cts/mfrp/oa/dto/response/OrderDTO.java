package com.cts.mfrp.oa.dto.response;

import com.cts.mfrp.oa.model.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDTO(
        Integer orderId,
        String tokenNumber,
        OrderStatus status,
        Float totalAmount,
        LocalDateTime orderTime,
        LocalDateTime readyTime,
        UserResponse user,
        VendorDTO vendor,
        List<OrderItemDTO> orderItems
) {}