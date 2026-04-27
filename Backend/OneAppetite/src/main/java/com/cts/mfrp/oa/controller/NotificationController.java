package com.cts.mfrp.oa.controller;

import com.cts.mfrp.oa.dto.response.NotificationResponse;
import com.cts.mfrp.oa.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<NotificationResponse>> getNotifications(@PathVariable Integer userId) {
        return ResponseEntity.ok(notificationService.getRecent(userId));
    }

    @GetMapping("/{userId}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable Integer userId) {
        return ResponseEntity.ok(Map.of("count", notificationService.unreadCount(userId)));
    }

    @PutMapping("/{userId}/mark-all-read")
    public ResponseEntity<Map<String, Object>> markAllRead(@PathVariable Integer userId) {
        return ResponseEntity.ok(notificationService.markAllAsRead(userId));
    }
}
