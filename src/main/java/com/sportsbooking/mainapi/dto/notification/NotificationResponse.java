package com.sportsbooking.mainapi.dto.notification;

import com.sportsbooking.mainapi.jpa.entity.enums.NotificationType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String title,
        String body,
        NotificationType type,
        UUID relatedId,
        boolean isRead,
        OffsetDateTime createdAt
) {
}
