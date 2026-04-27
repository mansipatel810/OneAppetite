package com.cts.mfrp.oa.dto.response;

import java.util.List;

public record VendorLocationsResponse(
        Integer vendorId,
        BuildingDTO primaryBuilding,
        List<BuildingDTO> additionalBuildings
) {
    public record BuildingDTO(
            Integer buildingId,
            String buildingName,
            Integer campusId,
            String campusName,
            Integer cityId,
            String cityName
    ) {}
}
