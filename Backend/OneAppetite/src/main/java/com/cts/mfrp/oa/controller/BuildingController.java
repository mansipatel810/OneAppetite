package com.cts.mfrp.oa.controller;

import com.cts.mfrp.oa.model.Building;
import com.cts.mfrp.oa.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/buildings")
@RequiredArgsConstructor
public class BuildingController {
    private final LocationService locationService;

    @GetMapping("/{campusId}")
    public ResponseEntity<List<Building>> getBuildings(@PathVariable Integer campusId) {
        return ResponseEntity.ok(locationService.getBuildingsByCampus(campusId));
    }
}