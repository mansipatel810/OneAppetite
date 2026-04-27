package com.cts.mfrp.oa.service;

import com.cts.mfrp.oa.dto.response.NotificationResponse;
import com.cts.mfrp.oa.model.Notification;
import com.cts.mfrp.oa.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private final NotificationRepository repo;

    public NotificationService(NotificationRepository repo) {
        this.repo = repo;
    }

    /** Persist a notification. Failures are swallowed so they never break the parent flow. */
    public void push(Integer userId, String message) {
        if (userId == null || message == null || message.isBlank()) return;
        try {
            repo.save(new Notification(userId, message));
        } catch (Exception ignored) {
            // best-effort; never let a notification failure abort the parent transaction
        }
    }

    public List<NotificationResponse> getRecent(Integer userId) {
        return repo.findTop20ByUserIdOrderByTimestampDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public long unreadCount(Integer userId) {
        return repo.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public Map<String, Object> markAllAsRead(Integer userId) {
        int updated = repo.markAllAsRead(userId);
        return Map.of("updated", updated);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getUserId(), n.getMessage(),
                n.getTimestamp(), n.getIsRead()
        );
    }
}
