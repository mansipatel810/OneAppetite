package com.cts.mfrp.oa.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateVendorLocationsRequest(
        @NotNull(message = "buildingIds must be provided (use [] to clear all extras)")
        List<Integer> buildingIds
) {}
