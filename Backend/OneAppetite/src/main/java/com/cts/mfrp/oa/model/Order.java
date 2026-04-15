package com.cts.mfrp.oa.model;



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

