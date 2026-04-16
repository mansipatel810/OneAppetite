package com.cts.mfrp.oa.model;

<<<<<<< HEAD
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
    @JoinColumn(name = "employee_id") // Your ERD uses employee_id for the user
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
=======


import jakarta.persistence.*;

@Entity
@Table(name = "orders")   // match the DB table name
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")   // match DB column
    private Long id;

    // Map to the existing 'status' column in DB
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OrderStatus status;

    // Optional: If you want to use other columns from DB, add them here
    // For example:
    // @Column(name = "total_amount")
    // private Float totalAmount;
    //
    // @Column(name = "order_time")
    // private java.time.LocalDateTime orderTime;
    //
    // @Column(name = "ready_time")
    // private java.time.LocalDateTime readyTime;
    //
    // @Column(name = "token_number")
    // private String tokenNumber;
    //
    // @Column(name = "employee_id")
    // private Integer employeeId;
    //
    // @Column(name = "user_id")
    // private Integer userId;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}

>>>>>>> 51ca6366f48c201655e89ee6b836107a5c8aeb83
