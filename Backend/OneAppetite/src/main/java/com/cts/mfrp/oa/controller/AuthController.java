package com.cts.mfrp.oa.controller;

import com.cts.mfrp.oa.dto.request.LoginRequest;
import com.cts.mfrp.oa.dto.request.RegisterRequest;
import com.cts.mfrp.oa.dto.request.VendorRegisterRequest;
import com.cts.mfrp.oa.dto.response.LoginResponse;
import com.cts.mfrp.oa.dto.response.UserResponse;
import com.cts.mfrp.oa.dto.response.VendorRegisterResponse;
import com.cts.mfrp.oa.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(201).body(authService.register(request));
    }

    @PostMapping("/register/vendor")
    public ResponseEntity<VendorRegisterResponse> registerVendor(@Valid @RequestBody VendorRegisterRequest request) {
        return ResponseEntity.status(201).body(authService.registerVendor(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader("Authorization") String authHeader) {
        authService.logout(authHeader.substring(7));
        return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
    }
}
