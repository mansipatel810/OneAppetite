package com.cts.mfrp.oa.dto.response;

import com.cts.mfrp.oa.model.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;

public record CartResponseDTO(
        Integer orderId,
        String tokenNumber,
        OrderStatus status,
        Float totalAmount,
        LocalDateTime orderTime,
        LocalDateTime readyTime,
        VendorDTO vendor,
        List<OrderItemDTO> items
) {}