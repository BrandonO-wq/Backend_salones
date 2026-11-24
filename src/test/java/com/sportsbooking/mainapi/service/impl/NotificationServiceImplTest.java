package com.sportsbooking.mainapi.service.impl;

import com.sportsbooking.mainapi.components.PushNotificationComponent;
import com.sportsbooking.mainapi.jpa.entity.NotificationEntity;
import com.sportsbooking.mainapi.jpa.entity.enums.NotificationType;
import com.sportsbooking.mainapi.jpa.repository.NotificationRepository;
import com.sportsbooking.mainapi.jpa.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private PushNotificationComponent pushNotificationComponent;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void testNotifyDebt_SanitizesMojibakeBeforeSavingAndPushing() {
        when(userRepository.existsById(userId)).thenReturn(true);

        String mojibakeTitle = "âš ï¸ Recordatorio de deuda";
        String mojibakeBody = "Tienes un retraso de 3 dÃas en el pago.";

        notificationService.notifyDebt(userId, mojibakeTitle, mojibakeBody);

        ArgumentCaptor<NotificationEntity> entityCaptor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository).save(entityCaptor.capture());

        NotificationEntity savedEntity = entityCaptor.getValue();
        assertEquals("⚠️ Recordatorio de deuda", savedEntity.getTitle());
        assertEquals("Tienes un retraso de 3 días en el pago.", savedEntity.getBody());
        assertEquals(NotificationType.DEBT_ALERT, savedEntity.getType());

        verify(pushNotificationComponent).sendToUser(
                eq(userId),
                eq("⚠️ Recordatorio de deuda"),
                eq("Tienes un retraso de 3 días en el pago."),
                anyMap()
        );
    }
}

