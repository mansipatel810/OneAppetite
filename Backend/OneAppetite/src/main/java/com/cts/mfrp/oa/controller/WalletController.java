package com.cts.mfrp.oa.controller;

import com.cts.mfrp.oa.dto.request.TopUpRequest;
import com.cts.mfrp.oa.dto.response.WalletResponse;
import com.cts.mfrp.oa.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<WalletResponse> getBalance(@PathVariable Integer userId) {
        return ResponseEntity.ok(walletService.getBalance(userId));
    }

    @PostMapping("/{userId}/topup")
    public ResponseEntity<WalletResponse> topUp(
            @PathVariable Integer userId,
            @Valid @RequestBody TopUpRequest request) {
        return ResponseEntity.ok(walletService.topUp(userId, request));
    }
}
