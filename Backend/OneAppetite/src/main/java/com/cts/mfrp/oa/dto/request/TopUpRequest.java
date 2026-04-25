package com.cts.mfrp.oa.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record TopUpRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "1.0", message = "Minimum top-up amount is 1")
        @DecimalMax(value = "50000.0", message = "Maximum top-up amount per transaction is 50000")
        Double amount,

        @NotNull(message = "UPI ID is required")
        @Pattern(regexp = "^[\\w.\\-]+@[\\w.\\-]+$", message = "Invalid UPI ID format")
        String upiId
) {}
