package com.cts.mfrp.oa.dto.response;

public record MenuItemResponse(
        Integer itemId,
        String itemName,
        String category,
        Double price,
        Integer quantityAvailable,
        Boolean isInStock,
        String imageUrl,
        Integer vendorId,
        String vendorName,
        String vendorDescription
) {}
