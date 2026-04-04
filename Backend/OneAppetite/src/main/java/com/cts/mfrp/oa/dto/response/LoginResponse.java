package com.cts.mfrp.oa.dto.response;

public record LoginResponse(
        Integer userId,
        String name,
        String email,
        String role
) {}
