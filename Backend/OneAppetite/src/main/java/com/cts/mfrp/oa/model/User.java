package com.cts.mfrp.oa.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Column(name = "vendor_name", length = 100)
    private String vendorName;

    @Column(name = "vendor_description", length = 255)
    private String vendorDescription;

    @Column(name = "vendor_image_url", length = 500)
    private String vendorImageUrl;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "wallet_balance", nullable = false)
    private Double walletBalance = 0.0;

    @Column(name = "vendor_type", length = 20)
    private VendorType vendorType;

    @Column(name = "daily_topup_date")
    private java.time.LocalDate dailyTopUpDate;

    @Column(name = "daily_topup_total")
    private Double dailyTopUpTotal = 0.0;

}
