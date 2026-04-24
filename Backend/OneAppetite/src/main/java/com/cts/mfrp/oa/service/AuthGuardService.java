package com.cts.mfrp.oa.service;

import com.cts.mfrp.oa.exception.InvalidCredentialsException;
import com.cts.mfrp.oa.model.Role;
import com.cts.mfrp.oa.model.User;
import com.cts.mfrp.oa.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthGuardService {

    private final UserRepository userRepository;

    public AuthGuardService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User verifyCaller(Integer callerId) {
        if (callerId == null) {
            throw new InvalidCredentialsException("Missing X-User-Id header.");
        }
        User caller = userRepository.findById(callerId)
                .orElseThrow(() -> new InvalidCredentialsException("User not found."));
        if (!Boolean.TRUE.equals(caller.getIsActive())) {
            throw new InvalidCredentialsException("User account is disabled.");
        }
        return caller;
    }

    public User verifyAdmin(Integer callerId) {
        User caller = verifyCaller(callerId);
        if (caller.getRole() != Role.ADMIN) {
            throw new InvalidCredentialsException("Admin access required.");
        }
        return caller;
    }

    public User verifyVendor(Integer callerId) {
        User caller = verifyCaller(callerId);
        if (caller.getRole() != Role.VENDOR) {
            throw new InvalidCredentialsException("Vendor access required.");
        }
        return caller;
    }

    public User verifyEmployee(Integer callerId) {
        User caller = verifyCaller(callerId);
        if (caller.getRole() != Role.EMPLOYEE) {
            throw new InvalidCredentialsException("Employee access required.");
        }
        return caller;
    }

    public User verifySelfOrAdmin(Integer callerId, Integer targetUserId) {
        User caller = verifyCaller(callerId);
        if (caller.getRole() == Role.ADMIN) {
            return caller;
        }
        if (targetUserId == null || !targetUserId.equals(callerId)) {
            throw new InvalidCredentialsException("You are not allowed to access this resource.");
        }
        return caller;
    }

    public User verifyVendorSelf(Integer callerId, Integer targetVendorId) {
        User caller = verifyVendor(callerId);
        if (targetVendorId == null || !targetVendorId.equals(callerId)) {
            throw new InvalidCredentialsException("Vendors can only modify their own resources.");
        }
        return caller;
    }
}
