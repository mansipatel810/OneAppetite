package com.cts.mfrp.oa.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_user_time", columnList = "user_id, timestamp")
})
@Data
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    public Notification(Integer userId, String message) {
        this.userId = userId;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.isRead = false;
    }
}
