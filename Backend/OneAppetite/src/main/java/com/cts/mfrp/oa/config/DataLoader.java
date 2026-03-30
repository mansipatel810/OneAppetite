package com.cts.mfrp.oa.config;

import com.cts.mfrp.oa.model.*;
import com.cts.mfrp.oa.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final CityRepository cityRepo;
    private final CampusRepository campusRepo;
    private final BuildingRepository buildingRepo;

    @Override
    public void run(String... args) {
        if (cityRepo.count() == 0) {
            // 1. Add City
            City chennai = cityRepo.save(new City(null, "Chennai", null));

            // 2. Add Campuses
            Campus siruseri = campusRepo.save(new Campus(null, chennai, "Siruseri SIPCOT", "SIPCOT IT Park, Siruseri", null));
            Campus mepz = campusRepo.save(new Campus(null, chennai, "MEPZ SEZ", "GST Road, West Tambaram", null));
            Campus sholinganallur = campusRepo.save(new Campus(null, chennai, "OMR-Sholinganallur", "KITS Campus, OMR", null));
            Campus tharamani = campusRepo.save(new Campus(null, chennai, "Tharamani CRC", "Ramanujan IT City", null));
            Campus dlf = campusRepo.save(new Campus(null, chennai, "DLF Cyber City", "Ramapuram, Mt Poonamallee Rd", null));

            // 3. Add Buildings for Siruseri
            buildingRepo.save(new Building(null, siruseri, "Cafeteria Block"));
            buildingRepo.save(new Building(null, siruseri, "Academy Block"));

            // 4. Add Buildings for MEPZ (Tambaram)
            buildingRepo.save(new Building(null, mepz, "SDB-1"));
            buildingRepo.save(new Building(null, mepz, "SDB-2"));
            buildingRepo.save(new Building(null, mepz, "SDB-3"));
            buildingRepo.save(new Building(null, mepz, "Central Canteen Building"));

            // 5. Add Buildings for Sholinganallur
            buildingRepo.save(new Building(null, sholinganallur, "Building 1 - Tower A"));
            buildingRepo.save(new Building(null, sholinganallur, "Building 2 - Tower B"));

            // 6. Add Buildings for Tharamani
            buildingRepo.save(new Building(null, tharamani, "Hardy Tower"));
            buildingRepo.save(new Building(null, tharamani, "Carrand Tower"));

            // 7. Add Buildings for DLF
            buildingRepo.save(new Building(null, dlf, "Block 5 Food Court"));
            buildingRepo.save(new Building(null, dlf, "Block 7"));

            System.out.println("OneAppetite: Chennai Campus Hierarchy Seeded Successfully!");
        }
    }
}