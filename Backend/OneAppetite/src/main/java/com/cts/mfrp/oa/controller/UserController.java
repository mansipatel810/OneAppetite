package com.cts.mfrp.oa.controller;

import com.cts.mfrp.oa.dto.request.ChangePasswordRequest;
import com.cts.mfrp.oa.dto.request.UpdateProfileRequest;
import com.cts.mfrp.oa.dto.request.UpdateVendorLocationsRequest;
import com.cts.mfrp.oa.dto.response.UserProfileResponse;
import com.cts.mfrp.oa.dto.response.VendorLocationsResponse;
import com.cts.mfrp.oa.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getProfile(@PathVariable Integer userId) {
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @PathVariable Integer userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    @PutMapping("/{userId}/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @PathVariable Integer userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    @PutMapping("/{userId}/notifications")
    public ResponseEntity<UserProfileResponse> setNotifications(
            @PathVariable Integer userId,
            @RequestParam("enabled") Boolean enabled) {
        return ResponseEntity.ok(userService.setNotifications(userId, enabled));
    }

    /* ── Vendor stall-locations (vendor only) ────────────────── */

    @GetMapping("/{vendorId}/locations")
    public ResponseEntity<VendorLocationsResponse> getVendorLocations(@PathVariable Integer vendorId) {
        return ResponseEntity.ok(userService.getVendorLocations(vendorId));
    }

    @PutMapping("/{vendorId}/locations")
    public ResponseEntity<VendorLocationsResponse> updateVendorLocations(
            @PathVariable Integer vendorId,
            @Valid @RequestBody UpdateVendorLocationsRequest request) {
        return ResponseEntity.ok(userService.updateVendorLocations(vendorId, request));
    }
}
