package com.cts.mfrp.oa.controller;

import com.cts.mfrp.oa.dto.response.UserResponse;
import com.cts.mfrp.oa.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/vendors")
    public ResponseEntity<List<UserResponse>> getAllVendors(
            @RequestHeader(value = "X-User-Id", required = false) Integer callerId) {
        adminService.verifyAdmin(callerId);
        return ResponseEntity.ok(adminService.getAllVendors());
    }

    @GetMapping("/employees")
    public ResponseEntity<List<UserResponse>> getAllEmployees(
            @RequestHeader(value = "X-User-Id", required = false) Integer callerId) {
        adminService.verifyAdmin(callerId);
        return ResponseEntity.ok(adminService.getAllEmployees());
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers(
            @RequestHeader(value = "X-User-Id", required = false) Integer callerId) {
        adminService.verifyAdmin(callerId);
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PutMapping("/users/{userId}/toggle-status")
    public ResponseEntity<UserResponse> toggleUserStatus(
            @RequestHeader(value = "X-User-Id", required = false) Integer callerId,
            @PathVariable Integer userId) {
        adminService.verifyAdmin(callerId);
        return ResponseEntity.ok(adminService.toggleUserStatus(callerId, userId));
    }
}
