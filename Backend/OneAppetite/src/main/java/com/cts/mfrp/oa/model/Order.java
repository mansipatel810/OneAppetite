package com.cts.mfrp.oa.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer orderId;

    @ManyToOne
    @JoinColumn(name = "employee_id") // ERD uses employee_id for the user
    private User user;

    @ManyToOne
    @JoinColumn(name = "vendor_id")
    private User vendor;

    private String tokenNumber;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private Float totalAmount;

    private LocalDateTime orderTime;
    private LocalDateTime readyTime;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<OrderItem> orderItems;

}


