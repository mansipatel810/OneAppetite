package com.cts.mfrp.oa.controller;

import com.cts.mfrp.oa.dto.response.VendorRegisterResponse;
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

    @GetMapping("/building/{buildingId}")
    public List<VendorRegisterResponse> getVendorsByBuilding(@PathVariable Integer buildingId) {
        return vendorService.getVendorsByBuilding(buildingId);
    }

    @PutMapping("/{vendorId}/image")
    public ResponseEntity<Map<String, String>> updateVendorImage(
            @PathVariable Integer vendorId,
            @RequestBody Map<String, String> request) {
        vendorService.updateVendorImage(vendorId, request.get("vendorImageUrl"));
        return ResponseEntity.ok(Map.of("message", "Vendor image updated successfully."));
    }
}
