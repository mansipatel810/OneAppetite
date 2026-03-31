package com.cts.mfrp.oa.controller;

import com.cts.mfrp.oa.dto.response.VendorRegisterResponse;
import com.cts.mfrp.oa.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vendors")
public class VendorController {

    @Autowired private VendorService vendorService;

    @GetMapping("/building/{buildingId}")
    public List<VendorRegisterResponse> getVendorsByBuilding(@PathVariable Integer buildingId) {
        return vendorService.getVendorsByBuilding(buildingId);
    }
}
