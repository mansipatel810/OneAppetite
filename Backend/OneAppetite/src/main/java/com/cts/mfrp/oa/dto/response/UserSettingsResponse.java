package com.cts.mfrp.oa.dto.response;

public record UserSettingsResponse(
        Integer userId,
        String name,
        String email,
        String phone,
        Double walletBalance,
        String dietaryPreference,
        Boolean notificationsEnabled
) {}