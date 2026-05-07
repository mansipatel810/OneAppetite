package com.cts.mfrp.oa.service;

import com.cts.mfrp.oa.dto.response.VendorRegisterResponse;
import com.cts.mfrp.oa.exception.ResourceNotFoundException;
import com.cts.mfrp.oa.model.Role;
import com.cts.mfrp.oa.model.User;
import com.cts.mfrp.oa.repository.BuildingRepository;
import com.cts.mfrp.oa.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class VendorService {

    private static final Logger log = LoggerFactory.getLogger(VendorService.class);

    @Autowired private BuildingRepository buildingRepo;
    @Autowired private UserRepository userRepo;

    public List<VendorRegisterResponse> getVendorsByBuilding(Integer buildingId) {
        // Visible diagnostic per the task hint — confirms the ID we received.
        System.out.println("[VendorService] getVendorsByBuilding called with buildingId=" + buildingId);
        log.info("getVendorsByBuilding called with buildingId={}", buildingId);

        if (buildingId == null) {
            System.out.println("[VendorService] buildingId is null — returning empty");
            return Collections.emptyList();
        }

        // Confirm the building actually exists. We log either way so the
        // operator can see what happened, but we never let the JPQL query
        // run against a null Building (which would silently return []).
        boolean buildingExists = buildingRepo.findById(buildingId).isPresent();
        System.out.println("[VendorService] building " + buildingId + " exists in DB: " + buildingExists);

        if (!buildingExists) {
            log.warn("Building {} not found — no vendors to return", buildingId);
            return Collections.emptyList();
        }

        // Use the explicit JPQL query that filters by FK column directly.
        // This is more robust than findByBuildingAndRole(Building, Role)
        // because it never depends on an attached Building entity.
        List<User> vendors = userRepo.findVendorsByBuildingId(buildingId, Role.VENDOR);
        System.out.println("[VendorService] vendors found for buildingId=" + buildingId
                + " → count=" + vendors.size());

        return vendors.stream().map(v -> new VendorRegisterResponse(
                v.getUserId(),
                v.getName(),
                v.getEmail(),
                v.getPhone(),
                v.getRole().name(),
                v.getVendorName(),
                v.getVendorDescription(),
                v.getBuilding() != null ? v.getBuilding().getBuildingId() : null,
                v.getVendorImageUrl(),
                v.getVendorType(),
                v.getStallFloor(),
                v.getStallWing()
        )).toList();
    }

    public void updateVendorImage(Integer vendorId, String vendorImageUrl) {
        User vendor = userRepo.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with ID: " + vendorId));
        vendor.setVendorImageUrl(vendorImageUrl);
        userRepo.save(vendor);
    }
}
