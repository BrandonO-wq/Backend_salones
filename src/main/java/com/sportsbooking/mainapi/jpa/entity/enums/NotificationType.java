package com.sportsbooking.mainapi.jpa.entity.enums;

/**
 * Tipos de notificaciones del sistema.
 */
public enum NotificationType {
    NEW_BOOKING,
    BOOKING_CONFIRMED,
    BOOKING_REJECTED,
    SYSTEM_INFO,
    HOST_PENDING_REMINDER,
    SYSTEM_AUTO_CONFIRMED,
    CLIENT_UPCOMING_REMINDER;
}
