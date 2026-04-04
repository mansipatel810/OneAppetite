package com.cts.mfrp.oa.dto.response;

public record VendorRegisterResponse(
        Integer userId,
        String name,
        String email,
        String phone,
        String role,
        String vendorName,
        String vendorDescription,
        Integer buildingId,
        String vendorImageUrl
) {}
