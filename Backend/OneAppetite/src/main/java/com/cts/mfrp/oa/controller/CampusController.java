package com.cts.mfrp.oa.controller;

import com.cts.mfrp.oa.model.Campus;
import com.cts.mfrp.oa.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/campuses")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CampusController {
    private final LocationService locationService;

    // 1. Existing: Fetch all campuses belonging to a specific city
    // Used for the "Select Campus" dropdown on Page 1
    @GetMapping("/city/{cityId}")
    public ResponseEntity<List<Campus>> getCampuses(@PathVariable Integer cityId) {
        return ResponseEntity.ok(locationService.getCampusesByCity(cityId));
    }

    // 2. New: Fetch details for a single campus by its ID
    // Used for the header on Page 2 (e.g., "Showing locations for Siruseri SIPCOT")
    @GetMapping("/{id}")
    public ResponseEntity<Campus> getCampusById(@PathVariable Integer id) {
        Campus campus = locationService.getCampusById(id);
        if (campus != null) {
            return ResponseEntity.ok(campus);
        }
        return ResponseEntity.notFound().build();
    }
}