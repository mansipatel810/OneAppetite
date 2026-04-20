package com.cts.mfrp.oa.controller;

import com.cts.mfrp.oa.model.City;
import com.cts.mfrp.oa.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cities")
@RequiredArgsConstructor
public class CityController {
    private final LocationService locationService;

    @GetMapping
    public ResponseEntity<List<City>> getCities() {
        return ResponseEntity.ok(locationService.getAllCities());
    }
}