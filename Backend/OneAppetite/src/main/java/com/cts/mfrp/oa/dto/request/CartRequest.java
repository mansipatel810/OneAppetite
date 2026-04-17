package com.cts.mfrp.oa.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartRequest(
        @NotNull Integer userId,
        @NotNull Integer menuItemId,
        @Min(1) Integer quantity
) {}
