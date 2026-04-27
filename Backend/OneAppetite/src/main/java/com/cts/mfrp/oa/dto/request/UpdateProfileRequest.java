package com.cts.mfrp.oa.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 1, max = 100, message = "Name must be 1–100 characters")
        String name,

        @Pattern(regexp = "^\\d{10}$", message = "Phone must be 10 digits")
        String phone
) {}
