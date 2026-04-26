package com.cts.mfrp.oa.dto.request;

public record UserSettingsRequest(
        String dietaryPreference,
        Boolean notificationsEnabled
) {}