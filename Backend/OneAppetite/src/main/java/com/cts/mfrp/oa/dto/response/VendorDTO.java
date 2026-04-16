package com.cts.mfrp.oa.dto.response;

public record VendorDTO(
        Integer userId,
        String name,
        String email,
        String phone,
        Boolean isActive,
        String vendorName
) {}