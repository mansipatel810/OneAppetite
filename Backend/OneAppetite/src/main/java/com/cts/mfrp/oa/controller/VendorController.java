package com.cts.mfrp.oa.controller;

import com.cts.mfrp.oa.dto.response.VendorRegisterResponse;
import com.cts.mfrp.oa.model.VendorType;
import com.cts.mfrp.oa.service.AuthGuardService;
import com.cts.mfrp.oa.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/vendors")
public class VendorController {

    @Autowired private VendorService vendorService;
    @Autowired private AuthGuardService authGuard;

    @GetMapping("/building/{buildingId}")
    public List<VendorRegisterResponse> getVendorsByBuilding(@PathVariable Integer buildingId) {
        return vendorService.getVendorsByBuilding(buildingId);
    }

    @PutMapping("/{vendorId}/image")
    public ResponseEntity<Map<String, String>> updateVendorImage(
            @RequestHeader(value = "X-User-Id", required = false) Integer callerId,
            @PathVariable Integer vendorId,
            @RequestBody Map<String, String> request) {
        authGuard.verifyVendorSelf(callerId, vendorId);
        vendorService.updateVendorImage(vendorId, request.get("vendorImageUrl"));
        return ResponseEntity.ok(Map.of("message", "Vendor image updated successfully."));
    }
    @GetMapping("/veg/{buildingId}")
    public List<VendorRegisterResponse> getVegVendors(@PathVariable Integer buildingId) {
        return vendorService.getVendorsByBuildingAndType(buildingId, VendorType.VEG);
    }

    @GetMapping("/nonveg/{buildingId}")
    public List<VendorRegisterResponse> getNonVegVendors(@PathVariable Integer buildingId) {
        return vendorService.getVendorsByBuildingAndType(buildingId, VendorType.NON_VEG);
    }



}
