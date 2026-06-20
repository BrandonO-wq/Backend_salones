package com.sportsbooking.mainapi.components;

import com.google.firebase.messaging.*;
import com.sportsbooking.mainapi.jpa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Componente utilitario para el envío de notificaciones push via Firebase Cloud Messaging (FCM).
 *
 * <p>No forma parte del dominio de negocio; es una utilidad técnica de infraestructura.
 * Puede inyectarse en cualquier Service de negocio que necesite enviar notificaciones push.
 *
 * <h3>Formato de notificación</h3>
 * <ul>
 *   <li>Android: canal {@code canchago_high_importance_channel}, prioridad HIGH → heads-up garantizado.</li>
 *   <li>iOS (APNs): priority 10, sound {@code default}, badge 1.</li>
 *   <li>Data payload opcional: clave-valor para navegación en Flutter (ej: {@code route → "/reservations"}).</li>
 * </ul>
 *
 * <h3>Manejo de tokens inválidos</h3>
 * Si FCM responde con {@code UNREGISTERED} (app desinstalada o token expirado),
 * el campo {@code fb_token} del usuario se limpia automáticamente en BD para
 * evitar reintentos sobre tokens muertos.
 *
 * <h3>Ejemplo de uso</h3>
 * <pre>{@code
 * // Sin data payload
 * pushNotificationComponent.sendToUser(userId, "Nueva reserva", "Tu cancha está confirmada.");
 *
 * // Con data payload para navegación Flutter
 * pushNotificationComponent.sendToUser(userId, "Nueva reserva", "Tu cancha está confirmada.",
 *     Map.of("route", "/reservations", "reservationId", id.toString()));
 * }</pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PushNotificationComponent {

    private static final String ANDROID_CHANNEL_ID = "canchago_high_importance_channel";

    private final UserRepository userRepository;

    // ──────────────────────────────────────────────────────────────────────────
    // API pública
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Envía una notificación push al usuario identificado por {@code userId}.
     *
     * <p>Resuelve el {@code fbToken} del usuario desde la BD. Si no tiene token
     * registrado, loguea un warning y no envía nada.
     *
     * @param userId ID del usuario destino.
     * @param title  Título de la notificación (visible en la bandeja del dispositivo).
     * @param body   Cuerpo del mensaje.
     */
    public void sendToUser(UUID userId, String title, String body) {
        sendToUser(userId, title, body, null);
    }

    /**
     * Envía una notificación push al usuario con datos adicionales en el payload.
     *
     * @param userId ID del usuario destino.
     * @param title  Título de la notificación.
     * @param body   Cuerpo del mensaje.
     * @param data   Map de clave-valor para el data payload (ej: {@code "route" → "/reservations"}).
     *               Puede ser {@code null} o vacío.
     */
    public void sendToUser(UUID userId, String title, String body, Map<String, String> data) {
        var user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            log.warn("[FCM] Usuario {} no encontrado — notificación cancelada.", userId);
            return;
        }

        String token = user.getFbToken();
        if (token == null || token.isBlank()) {
            log.warn("[FCM] Usuario {} no tiene fb_token registrado — notificación cancelada.", userId);
            return;
        }

        sendToToken(token, title, body, data);
    }

    /**
     * Envía una notificación push directamente a un token FCM.
     *
     * <p>Si el token es {@code UNREGISTERED}, se limpia en la BD automáticamente.
     *
     * @param fcmToken token FCM del dispositivo destino.
     * @param title    Título de la notificación.
     * @param body     Cuerpo del mensaje.
     * @param data     Data payload opcional (puede ser {@code null}).
     */
    public void sendToToken(String fcmToken, String title, String body, Map<String, String> data) {
        Message message = buildMessage(fcmToken, title, body, data);
        try {
            String messageId = FirebaseMessaging.getInstance().send(message);
            log.info("[FCM] Notificación enviada → messageId: {}", messageId);
        } catch (FirebaseMessagingException e) {
            handleFirebaseException(e, fcmToken, title);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Internos
    // ──────────────────────────────────────────────────────────────────────────

    private Message buildMessage(String fcmToken, String title, String body, Map<String, String> data) {
        Message.Builder builder = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder()
                                .setChannelId(ANDROID_CHANNEL_ID)
                                .setSound("default")
                                .build())
                        .build())
                .setApnsConfig(ApnsConfig.builder()
                        .putHeader("apns-priority", "10")
                        .setAps(Aps.builder()
                                .setAlert(ApsAlert.builder()
                                        .setTitle(title)
                                        .setBody(body)
                                        .build())
                                .setSound("default")
                                .setBadge(1)
                                .build())
                        .build());

        if (data != null && !data.isEmpty()) {
            builder.putAllData(data);
        }

        return builder.build();
    }

    private void handleFirebaseException(FirebaseMessagingException e, String fcmToken, String title) {
        if (MessagingErrorCode.UNREGISTERED.equals(e.getMessagingErrorCode())) {
            log.warn("[FCM] Token UNREGISTERED → limpiando fb_token. Token: {}...",
                    fcmToken.length() > 20 ? fcmToken.substring(0, 20) : fcmToken);
            userRepository.findByFbToken(fcmToken).ifPresent(user -> {
                user.setFbToken(null);
                userRepository.save(user);
                log.info("[FCM] fb_token limpiado para usuario ID: {}", user.getId());
            });
        } else {
            log.error("[FCM] Error enviando '{}': {} — code: {}", title, e.getMessage(), e.getMessagingErrorCode());
        }
    }
}
