package com.sportsbooking.mainapi.service.impl;

import com.sportsbooking.mainapi.components.PushNotificationComponent;
import com.sportsbooking.mainapi.dto.common.PaginatedResponse;
import com.sportsbooking.mainapi.dto.notification.NotificationResponse;
import com.sportsbooking.mainapi.jpa.entity.NotificationEntity;
import com.sportsbooking.mainapi.jpa.entity.enums.NotificationType;
import com.sportsbooking.mainapi.jpa.repository.NotificationRepository;
import com.sportsbooking.mainapi.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final PushNotificationComponent pushNotificationComponent;

    @Override
    @Transactional
    public void sendPushAndSave(UUID userId, String title, String body, NotificationType type, UUID relatedId, Map<String, String> data) {
        log.info("[NotificationService] Registrando notificación para userId: {} ({})", userId, type);

        // 1. Guardar en BD para historial (la campanita)
        NotificationEntity entity = NotificationEntity.builder()
                .userId(userId)
                .title(title)
                .body(body)
                .type(type)
                .relatedId(relatedId)
                .isRead(false)
                .build();
        notificationRepository.save(entity);

        // 2. Disparar FCM (puede fallar, no debe afectar el commit DB)
        try {
            pushNotificationComponent.sendToUser(userId, title, body, data);
        } catch (Exception e) {
            log.warn("[NotificationService] Fallo al enviar FCM a {}: {}", userId, e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUserId().equals(userId)) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<NotificationResponse> getUserNotifications(UUID userId, Pageable pageable) {
        Page<NotificationResponse> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(n -> new NotificationResponse(
                        n.getId(),
                        n.getTitle(),
                        n.getBody(),
                        n.getType(),
                        n.getRelatedId(),
                        n.isRead(),
                        n.getCreatedAt()
                ));
        return PaginatedResponse.from(page);
    }
}
