package com.cts.mfrp.oa.service;

import com.cts.mfrp.oa.dto.response.VendorRegisterResponse;
import com.cts.mfrp.oa.exception.ResourceNotFoundException;
import com.cts.mfrp.oa.model.Building;
import com.cts.mfrp.oa.model.Role;
import com.cts.mfrp.oa.model.User;
import com.cts.mfrp.oa.repository.BuildingRepository;
import com.cts.mfrp.oa.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VendorService {

    @Autowired private BuildingRepository buildingRepo;
    @Autowired private UserRepository userRepo;

    public List<VendorRegisterResponse> getVendorsByBuilding(Integer buildingId) {
        Building building = buildingRepo.findById(buildingId).orElse(null);
        List<User> vendors = userRepo.findByBuildingAndRole(building, Role.VENDOR);

        return vendors.stream().map(v -> new VendorRegisterResponse(
                v.getUserId(),
                v.getName(),
                v.getEmail(),
                v.getPhone(),
                v.getRole().name(),
                v.getVendorName(),
                v.getVendorDescription(),
                v.getBuilding().getBuildingId(),
                v.getVendorImageUrl()
        )).toList();
    }

    public void updateVendorImage(Integer vendorId, String vendorImageUrl) {
        User vendor = userRepo.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with ID: " + vendorId));
        vendor.setVendorImageUrl(vendorImageUrl);
        userRepo.save(vendor);
    }
}
