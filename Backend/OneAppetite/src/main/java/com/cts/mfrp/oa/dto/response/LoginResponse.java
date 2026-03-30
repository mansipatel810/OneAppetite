package com.cts.mfrp.oa.dto.response;

public record LoginResponse(
        String token,
        Integer userId,
        String name,
        String email,
        String role
) {}
