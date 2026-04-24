package com.cts.mfrp.oa.controller;

import com.cts.mfrp.oa.dto.request.TopUpRequest;
import com.cts.mfrp.oa.dto.response.WalletResponse;
import com.cts.mfrp.oa.service.AuthGuardService;
import com.cts.mfrp.oa.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;
    private final AuthGuardService authGuard;

    public WalletController(WalletService walletService, AuthGuardService authGuard) {
        this.walletService = walletService;
        this.authGuard = authGuard;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<WalletResponse> getBalance(
            @RequestHeader(value = "X-User-Id", required = false) Integer callerId,
            @PathVariable Integer userId) {
        authGuard.verifySelfOrAdmin(callerId, userId);
        return ResponseEntity.ok(walletService.getBalance(userId));
    }

    @PostMapping("/{userId}/topup")
    public ResponseEntity<WalletResponse> topUp(
            @RequestHeader(value = "X-User-Id", required = false) Integer callerId,
            @PathVariable Integer userId,
            @Valid @RequestBody TopUpRequest request) {
        authGuard.verifySelfOrAdmin(callerId, userId);
        return ResponseEntity.ok(walletService.topUp(userId, request));
    }
}
