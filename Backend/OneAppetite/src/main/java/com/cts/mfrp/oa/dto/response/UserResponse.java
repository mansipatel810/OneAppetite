package com.cts.mfrp.oa.dto.response;

public record UserResponse(
        Integer userId,
        String name,
        String email,
        String phone,
        String role
) {}
