package com.cts.mfrp.oa.service;

import com.cts.mfrp.oa.dto.response.UserResponse;
import com.cts.mfrp.oa.exception.ResourceNotFoundException;
import com.cts.mfrp.oa.model.Role;
import com.cts.mfrp.oa.model.User;
import com.cts.mfrp.oa.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;

    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponse> getAllVendors() {
        return userRepository.findByRole(Role.VENDOR)
                .stream().map(this::toResponse).toList();
    }

    public List<UserResponse> getAllEmployees() {
        return userRepository.findByRole(Role.EMPLOYEE)
                .stream().map(this::toResponse).toList();
    }

    public UserResponse toggleUserStatus(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        user.setIsActive(!user.getIsActive());
        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name(),
                user.getIsActive()
        );
    }
}
