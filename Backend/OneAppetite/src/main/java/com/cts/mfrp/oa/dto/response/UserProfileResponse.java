package com.cts.mfrp.oa.dto.response;

public record UserProfileResponse(
        Integer userId,
        String name,
        String email,
        String phone,
        String role,
        Boolean isActive,
        Double walletBalance,
        Boolean notificationsEnabled,
        Integer buildingId,
        String buildingName
) {}
