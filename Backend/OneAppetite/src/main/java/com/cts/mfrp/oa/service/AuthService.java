package com.cts.mfrp.oa.service;

import com.cts.mfrp.oa.dto.request.LoginRequest;
import com.cts.mfrp.oa.dto.request.RegisterRequest;
import com.cts.mfrp.oa.dto.request.VendorRegisterRequest;
import com.cts.mfrp.oa.dto.response.LoginResponse;
import com.cts.mfrp.oa.dto.response.UserResponse;
import com.cts.mfrp.oa.dto.response.VendorRegisterResponse;
import com.cts.mfrp.oa.exception.EmailAlreadyExistsException;
import com.cts.mfrp.oa.exception.InvalidCredentialsException;
import com.cts.mfrp.oa.exception.InvalidEmailDomainException;
import com.cts.mfrp.oa.exception.ResourceNotFoundException;
import com.cts.mfrp.oa.model.Building;
import com.cts.mfrp.oa.model.Role;
import com.cts.mfrp.oa.model.User;
import com.cts.mfrp.oa.repository.BuildingRepository;
import com.cts.mfrp.oa.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthService {

    private static final Set<String> ALLOWED_DOMAINS = Set.of("cognizant.com", "cts.com");

    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository;

    public AuthService(UserRepository userRepository, BuildingRepository buildingRepository) {
        this.userRepository = userRepository;
        this.buildingRepository = buildingRepository;
    }

    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email already registered: " + request.email());
        }

        String domain = request.email().substring(request.email().indexOf('@') + 1).toLowerCase();
        if (!ALLOWED_DOMAINS.contains(domain)) {
            throw new InvalidEmailDomainException("Email domain not allowed. Use a corporate email.");
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setPassword(BCrypt.hashpw(request.password(), BCrypt.gensalt()));
        user.setRole(Role.EMPLOYEE);
        user.setIsActive(true);

        User saved = userRepository.save(user);

        return new UserResponse(
                saved.getUserId(),
                saved.getName(),
                saved.getEmail(),
                saved.getPhone(),
                saved.getRole().name()
        );
    }

    public VendorRegisterResponse registerVendor(VendorRegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email already registered: " + request.email());
        }

        String domain = request.email().substring(request.email().indexOf('@') + 1).toLowerCase();
        if (!ALLOWED_DOMAINS.contains(domain)) {
            throw new InvalidEmailDomainException("Email domain not allowed. Use a corporate email.");
        }

        Building building = buildingRepository.findById(request.buildingId())
                .orElseThrow(() -> new ResourceNotFoundException("Building not found with ID: " + request.buildingId()));

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setPassword(BCrypt.hashpw(request.password(), BCrypt.gensalt()));
        user.setVendorName(request.vendorName());
        user.setVendorDescription(request.vendorDescription());
        user.setVendorImageUrl(request.vendorImageUrl());
        user.setBuilding(building);
        user.setRole(Role.VENDOR);
        user.setIsActive(true);

        User saved = userRepository.save(user);

        return new VendorRegisterResponse(
                saved.getUserId(),
                saved.getName(),
                saved.getEmail(),
                saved.getPhone(),
                saved.getRole().name(),
                saved.getVendorName(),
                saved.getVendorDescription(),
                saved.getBuilding().getBuildingId(),
                saved.getVendorImageUrl()
        );
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password."));

        if (!BCrypt.checkpw(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password.");
        }

        return new LoginResponse(user.getUserId(), user.getName(), user.getEmail(), user.getRole().name());
    }
}
