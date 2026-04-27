package com.cts.mfrp.oa.dto.response;

import java.time.LocalDateTime;

public record NotificationResponse(
        Integer id,
        Integer userId,
        String message,
        LocalDateTime timestamp,
        Boolean isRead
) {}
