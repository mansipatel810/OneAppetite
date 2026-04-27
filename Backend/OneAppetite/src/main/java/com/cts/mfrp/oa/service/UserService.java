package com.cts.mfrp.oa.service;

import com.cts.mfrp.oa.dto.request.ChangePasswordRequest;
import com.cts.mfrp.oa.dto.request.UpdateProfileRequest;
import com.cts.mfrp.oa.dto.request.UpdateVendorLocationsRequest;
import com.cts.mfrp.oa.dto.response.UserProfileResponse;
import com.cts.mfrp.oa.dto.response.VendorLocationsResponse;
import com.cts.mfrp.oa.dto.response.VendorLocationsResponse.BuildingDTO;
import com.cts.mfrp.oa.exception.InvalidCredentialsException;
import com.cts.mfrp.oa.exception.ResourceNotFoundException;
import com.cts.mfrp.oa.model.Building;
import com.cts.mfrp.oa.model.Role;
import com.cts.mfrp.oa.model.User;
import com.cts.mfrp.oa.repository.BuildingRepository;
import com.cts.mfrp.oa.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository;

    public UserService(UserRepository userRepository,
                       BuildingRepository buildingRepository) {
        this.userRepository = userRepository;
        this.buildingRepository = buildingRepository;
    }

    public UserProfileResponse getProfile(Integer userId) {
        User user = loadUser(userId);
        return toProfile(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(Integer userId, UpdateProfileRequest req) {
        User user = loadUser(userId);
        if (req.name() != null && !req.name().isBlank()) {
            user.setName(req.name().trim());
        }
        if (req.phone() != null && !req.phone().isBlank()) {
            user.setPhone(req.phone().trim());
        }
        return toProfile(userRepository.save(user));
    }

    @Transactional
    public void changePassword(Integer userId, ChangePasswordRequest req) {
        User user = loadUser(userId);
        if (!BCrypt.checkpw(req.currentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect.");
        }
        if (BCrypt.checkpw(req.newPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("New password must differ from current password.");
        }
        user.setPassword(BCrypt.hashpw(req.newPassword(), BCrypt.gensalt()));
        userRepository.save(user);
    }

    @Transactional
    public UserProfileResponse setNotifications(Integer userId, boolean enabled) {
        User user = loadUser(userId);
        user.setNotificationsEnabled(enabled);
        return toProfile(userRepository.save(user));
    }

    /* ── Vendor stall locations ──────────────────────────────── */

    public VendorLocationsResponse getVendorLocations(Integer vendorId) {
        User vendor = loadVendor(vendorId);
        return new VendorLocationsResponse(
                vendor.getUserId(),
                toBuildingDTO(vendor.getBuilding()),
                vendor.getAdditionalBuildings().stream()
                        .map(this::toBuildingDTO)
                        .toList()
        );
    }

    @Transactional
    public VendorLocationsResponse updateVendorLocations(Integer vendorId,
                                                         UpdateVendorLocationsRequest req) {
        User vendor = loadVendor(vendorId);

        // Build the new set, excluding the primary building (a vendor can't
        // "additionally" serve their own primary location).
        Integer primaryId = vendor.getBuilding() != null ? vendor.getBuilding().getBuildingId() : null;
        Set<Building> next = new HashSet<>();
        for (Integer id : new ArrayList<>(req.buildingIds() == null ? List.of() : req.buildingIds())) {
            if (id == null) continue;
            if (primaryId != null && id.equals(primaryId)) continue;
            buildingRepository.findById(id).ifPresent(next::add);
        }

        vendor.setAdditionalBuildings(next);
        userRepository.save(vendor);
        return getVendorLocations(vendorId);
    }

    private User loadVendor(Integer vendorId) {
        User user = loadUser(vendorId);
        if (user.getRole() != Role.VENDOR) {
            throw new InvalidCredentialsException("User is not a vendor.");
        }
        return user;
    }

    private BuildingDTO toBuildingDTO(Building b) {
        if (b == null) return null;
        Integer campusId    = b.getCampus() != null ? b.getCampus().getCampus_id() : null;
        String  campusName  = b.getCampus() != null ? b.getCampus().getCampusName() : null;
        Integer cityId      = (b.getCampus() != null && b.getCampus().getCity() != null)
                              ? b.getCampus().getCity().getCity_id() : null;
        String  cityName    = (b.getCampus() != null && b.getCampus().getCity() != null)
                              ? b.getCampus().getCity().getCityName() : null;
        return new BuildingDTO(
                b.getBuildingId(), b.getBuildingName(),
                campusId, campusName, cityId, cityName
        );
    }

    private User loadUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }

    private UserProfileResponse toProfile(User user) {
        Integer buildingId   = user.getBuilding() != null ? user.getBuilding().getBuildingId() : null;
        String  buildingName = user.getBuilding() != null ? user.getBuilding().getBuildingName() : null;
        return new UserProfileResponse(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name(),
                user.getIsActive(),
                user.getWalletBalance() == null ? 0.0 : user.getWalletBalance(),
                user.getNotificationsEnabled() == null ? Boolean.TRUE : user.getNotificationsEnabled(),
                buildingId,
                buildingName
        );
    }
}
