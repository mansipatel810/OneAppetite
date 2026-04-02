package com.cts.mfrp.oa.dto.request;

import jakarta.validation.constraints.*;

public record VendorRegisterRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "\\d{10}", message = "Phone must be exactly 10 digits")
        String phone,

        @NotBlank(message = "Password is required")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
                message = "Password must be at least 8 characters and include uppercase, lowercase, a digit, and a special character (@$!%*?&)"
        )
        String password,

        @NotBlank(message = "Vendor name is required")
        String vendorName,

        @NotBlank(message = "Vendor description is required")
        String vendorDescription,

        @NotNull(message = "Building ID is required")
        Integer buildingId,

        String vendorImageUrl
) {}
