package com.cts.mfrp.oa.service;

import com.cts.mfrp.oa.dto.request.ForgotPasswordRequest;
import com.cts.mfrp.oa.dto.request.LoginRequest;
import com.cts.mfrp.oa.dto.request.RegisterRequest;
import com.cts.mfrp.oa.dto.request.ResetPasswordRequest;
import com.cts.mfrp.oa.dto.request.VendorRegisterRequest;
import com.cts.mfrp.oa.dto.request.VerifyOtpRequest;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Service
public class AuthService {

    private static final Set<String> ALLOWED_DOMAINS = Set.of("cognizant.com", "cts.com");

    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository;
    private final String adminRegistrationSecret;

    public AuthService(UserRepository userRepository,
                       BuildingRepository buildingRepository,
                       @Value("${app.admin.registration-secret}") String adminRegistrationSecret) {
        this.userRepository = userRepository;
        this.buildingRepository = buildingRepository;
        this.adminRegistrationSecret = adminRegistrationSecret;
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
        if (request.role() != null && request.role().equalsIgnoreCase("VENDOR")) {
            throw new InvalidCredentialsException("Vendor registration requires /api/auth/register/vendor endpoint.");
        } else if (request.role() != null && request.role().equalsIgnoreCase("ADMIN")) {
//            if (request.adminSecret() == null || !adminRegistrationSecret.equals(request.adminSecret())) {
//                throw new InvalidCredentialsException("Invalid or missing admin registration secret.");
//            }
            user.setRole(Role.ADMIN);
        } else {
            user.setRole(Role.EMPLOYEE);
            user.setWalletBalance(1000.0);
        }
        user.setIsActive(true);

        User saved = userRepository.save(user);

        return new UserResponse(
                saved.getUserId(),
                saved.getName(),
                saved.getEmail(),
                saved.getPhone(),
                saved.getRole().name(),
                saved.getIsActive(),
                saved.getWalletBalance() == null ? 0.0 : saved.getWalletBalance()
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
                saved.getVendorImageUrl(),
                saved.getVendorType()
        );
    }

    /* ───────────────────────────────────────────────────────────
       Password reset (OTP) flow
       1) requestPasswordReset → 6-digit OTP, 10-minute window, "sent" via console
       2) verifyOtp            → confirms OTP without consuming it (UI gate)
       3) resetPassword        → re-validates OTP, BCrypt-hashes the new password
                                 and clears the OTP fields
    ─────────────────────────────────────────────────────────── */

    private static final int OTP_TTL_MINUTES = 10;
    private static final SecureRandom OTP_RNG = new SecureRandom();

    @Transactional
    public Map<String, String> requestPasswordReset(ForgotPasswordRequest request) {
        User user = userRepository.findByPhone(request.phone())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No account found for phone " + request.phone()));

        String otp = String.format("%06d", OTP_RNG.nextInt(1_000_000));
        user.setResetOtp(otp);
        user.setResetOtpExpiry(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES));
        userRepository.save(user);

        // ── Simulated SMS delivery ──
        // In production this would hand off to a real SMS provider (Twilio etc).
        System.out.println("====================================================");
        System.out.println("[OneAppetite] Password reset OTP for " + user.getName());
        System.out.println("    phone: " + user.getPhone());
        System.out.println("    OTP  : " + otp);
        System.out.println("    valid for " + OTP_TTL_MINUTES + " minutes");
        System.out.println("====================================================");

        return Map.of(
                "message", "OTP sent to your registered number",
                "expiresInMinutes", String.valueOf(OTP_TTL_MINUTES)
        );
    }

    public Map<String, Boolean> verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByPhone(request.phone())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No account found for phone " + request.phone()));

        boolean valid = isOtpValid(user, request.otp());
        if (!valid) {
            throw new InvalidCredentialsException("Invalid or expired OTP. Please request a new one.");
        }
        return Map.of("valid", true);
    }

    @Transactional
    public Map<String, String> resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByPhone(request.phone())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No account found for phone " + request.phone()));

        if (!isOtpValid(user, request.otp())) {
            throw new InvalidCredentialsException("Invalid or expired OTP. Please request a new one.");
        }

        // Hash via the same BCrypt utility used at registration
        user.setPassword(BCrypt.hashpw(request.newPassword(), BCrypt.gensalt()));
        // Burn the OTP so it can't be reused
        user.setResetOtp(null);
        user.setResetOtpExpiry(null);
        userRepository.save(user);

        return Map.of("message", "Password reset successfully");
    }

    private boolean isOtpValid(User user, String submitted) {
        if (user.getResetOtp() == null || user.getResetOtpExpiry() == null) return false;
        if (!user.getResetOtp().equals(submitted)) return false;
        return user.getResetOtpExpiry().isAfter(LocalDateTime.now());
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password."));

        if (!BCrypt.checkpw(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password.");
        }

        Role requestedRole;
        try {
            requestedRole = Role.valueOf(request.role().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidCredentialsException("Invalid role: " + request.role());
        }

        if (user.getRole() != requestedRole) {
            throw new InvalidCredentialsException(
                    "No " + requestedRole.name().toLowerCase() + " account found for this email. "
                            + "Your account is registered as " + user.getRole().name().toLowerCase() + "."
            );
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new InvalidCredentialsException("Your account has been deactivated. Please contact an administrator.");
        }
        return new LoginResponse(user.getUserId(), user.getName(), user.getEmail(), user.getRole().name());
    }
}