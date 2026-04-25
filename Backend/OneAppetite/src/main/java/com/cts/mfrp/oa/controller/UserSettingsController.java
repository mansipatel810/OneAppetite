package com.cts.mfrp.oa.controller;

import com.cts.mfrp.oa.dto.request.UserSettingsRequest;
import com.cts.mfrp.oa.dto.response.UserSettingsResponse;
import com.cts.mfrp.oa.service.UserSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserSettingsController {

    @Autowired private UserSettingsService userSettingsService;

    @GetMapping("/{userId}/settings")
    public UserSettingsResponse getSettings(@PathVariable Integer userId) {
        return userSettingsService.getSettings(userId);
    }

    @PutMapping("/{userId}/settings")
    public UserSettingsResponse updateSettings(@PathVariable Integer userId,
                                               @RequestBody UserSettingsRequest request) {
        return userSettingsService.updateSettings(userId, request);
    }

    @PutMapping("/{userId}/wallet/topup")
    public UserSettingsResponse topUpWallet(@PathVariable Integer userId,
                                             @RequestParam Double amount) {
        return userSettingsService.topUpWallet(userId, amount);
    }
}