package com.cts.mfrp.oa.service;

import com.cts.mfrp.oa.dto.request.UserSettingsRequest;
import com.cts.mfrp.oa.dto.response.UserSettingsResponse;
import com.cts.mfrp.oa.model.User;
import com.cts.mfrp.oa.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserSettingsService {

    @Autowired private UserRepository userRepository;

    private UserSettingsResponse toResponse(User user) {
        return new UserSettingsResponse(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getWalletBalance(),
                user.getDietaryPreference(),
                user.getNotificationsEnabled()
        );
    }

    // GET settings
    public UserSettingsResponse getSettings(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return toResponse(user);
    }

    // UPDATE dietary preference and notification toggle
    public UserSettingsResponse updateSettings(Integer userId, UserSettingsRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Always update dietary preference (can be null to clear it)
        user.setDietaryPreference(request.dietaryPreference());

        if (request.notificationsEnabled() != null) {
            user.setNotificationsEnabled(request.notificationsEnabled());
        }

        return toResponse(userRepository.save(user));
    }

    // WALLET top-up
    public UserSettingsResponse topUpWallet(Integer userId, Double amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setWalletBalance(user.getWalletBalance() + amount);
        return toResponse(userRepository.save(user));
    }
}