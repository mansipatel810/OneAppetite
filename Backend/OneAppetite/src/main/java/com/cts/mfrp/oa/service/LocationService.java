package com.cts.mfrp.oa.service;

import com.cts.mfrp.oa.model.*;
import com.cts.mfrp.oa.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {
    private final CityRepository cityRepository;
    private final CampusRepository campusRepository;
    private final BuildingRepository buildingRepository;

    public List<City> getAllCities() {
        return cityRepository.findAll();
    }

    public List<Campus> getCampusesByCity(Integer cityId) {
        return campusRepository.findByCityId(cityId);
    }

    public List<Building> getBuildingsByCampus(Integer campusId) {
        return buildingRepository.findByCampusId(campusId);
    }

    public Campus getCampusById(Integer id) {
        return campusRepository.findById(id).orElse(null);
    }
}