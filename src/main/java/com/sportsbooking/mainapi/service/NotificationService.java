package com.sportsbooking.mainapi.service;

import com.sportsbooking.mainapi.dto.common.PaginatedResponse;
import com.sportsbooking.mainapi.dto.notification.NotificationResponse;
import com.sportsbooking.mainapi.jpa.entity.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface NotificationService {

    void sendPushAndSave(UUID userId, String title, String body, NotificationType type, UUID relatedId, Map<String, String> data);

    void notifyDebt(UUID userId, String title, String body);

    long countUnread(UUID userId);

    void markAsRead(UUID notificationId, UUID userId);

    void markAllAsRead(UUID userId);

    PaginatedResponse<NotificationResponse> getUserNotifications(UUID userId, Pageable pageable);
}
