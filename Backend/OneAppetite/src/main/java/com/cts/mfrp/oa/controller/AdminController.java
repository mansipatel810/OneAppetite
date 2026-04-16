package com.cts.mfrp.oa.controller;

import com.cts.mfrp.oa.dto.response.UserResponse;
import com.cts.mfrp.oa.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/vendors")
    public ResponseEntity<List<UserResponse>> getAllVendors() {
        return ResponseEntity.ok(adminService.getAllVendors());
    }

    @GetMapping("/employees")
    public ResponseEntity<List<UserResponse>> getAllEmployees() {
        return ResponseEntity.ok(adminService.getAllEmployees());
    }

    @PutMapping("/users/{userId}/toggle-status")
    public ResponseEntity<UserResponse> toggleUserStatus(@PathVariable Integer userId) {
        return ResponseEntity.ok(adminService.toggleUserStatus(userId));
    }
}
