package com.cts.mfrp.oa.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "USERS")
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 10)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @ManyToOne
    @JoinColumn(name = "building_id")
    private Building building;

    /**
     * Additional buildings a vendor has chosen to also serve.
     * Their primary {@link #building} is the registration building; this
     * Set is the extra reach. The vendor query unions both at read time.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "vendor_extra_buildings",
            joinColumns        = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "building_id")
    )
    private Set<Building> additionalBuildings = new HashSet<>();

    @Column(name = "vendor_name", length = 100)
    private String vendorName;

    @Column(name = "vendor_description", length = 255)
    private String vendorDescription;

    @Column(name = "vendor_image_url", length = 500)
    private String vendorImageUrl;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "wallet_balance", nullable = false)
    private Double walletBalance = 1000.0;

    @Column(name = "notifications_enabled", nullable = false)
    private Boolean notificationsEnabled = true;

    @Column(name = "vendor_type", length = 20)
    private String vendorType;

    /* ── Vendor stall location (within their primary building) ── */
    @Column(name = "stall_floor", length = 16)
    private String stallFloor;     // e.g. "3", "Ground", "B1"

    @Column(name = "stall_wing", length = 16)
    private String stallWing;      // e.g. "A", "B", "North"

    /* ── Password reset (OTP) ──────────────────────────────────── */
    @Column(name = "reset_otp", length = 6)
    private String resetOtp;

    @Column(name = "reset_otp_expiry")
    private LocalDateTime resetOtpExpiry;

}
