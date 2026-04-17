package com.cts.mfrp.oa.dto.response;

public record MenuItemDTO(
        Integer itemId,
        String itemName,
        String category,
        Double price,
        Integer quantityAvailable,
        Boolean isInStock,
        VendorDTO vendor
) {}