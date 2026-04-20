package com.cts.mfrp.oa.dto.response;

public record OrderItemDTO(
        Integer orderItemId,
        Integer quantity,
        Float price,
        MenuItemDTO menuItem
) {}