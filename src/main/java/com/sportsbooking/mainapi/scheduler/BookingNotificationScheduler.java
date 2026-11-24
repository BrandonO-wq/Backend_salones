package com.sportsbooking.mainapi.scheduler;

import com.sportsbooking.mainapi.common.constants.RouteConstants;
import com.sportsbooking.mainapi.common.utils.DateUtilities;
import com.sportsbooking.mainapi.jpa.entity.BookingDetailEntity;
import com.sportsbooking.mainapi.jpa.entity.BookingEntity;
import com.sportsbooking.mainapi.jpa.entity.VenueEntity;
import com.sportsbooking.mainapi.jpa.entity.enums.BookingStatus;
import com.sportsbooking.mainapi.jpa.entity.enums.NotificationType;
import com.sportsbooking.mainapi.jpa.repository.BookingRepository;
import com.sportsbooking.mainapi.jpa.repository.NotificationRepository;
import com.sportsbooking.mainapi.jpa.repository.VenueRepository;
import com.sportsbooking.mainapi.service.BookingService;
import com.sportsbooking.mainapi.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scheduler.push.enabled", havingValue = "true", matchIfMissing = false)
public class BookingNotificationScheduler {

    private final BookingRepository bookingRepository;
    private final NotificationRepository notificationRepository;
    private final VenueRepository venueRepository;
    private final BookingService bookingService;
    private final NotificationService notificationService;

    @Value("${app.scheduler.push.host-reminder-minutes:120}")
    private int hostReminderMinutes;

    @Value("${app.scheduler.push.host-autoconfirm-minutes:60}")
    private int hostAutoConfirmMinutes;

    @Value("${app.scheduler.push.client-reminder-minutes:60}")
    private int clientReminderMinutes;

    private static final ZoneId LIMA_ZONE = ZoneId.of("America/Lima");

    @Scheduled(fixedDelayString = "${app.scheduler.push.interval:60}", timeUnit = TimeUnit.SECONDS)
    @Transactional
    public void processPushNotifications() {
        log.debug("[BookingNotificationScheduler] Iniciando barrido para notificaciones push...");

        LocalDate currentDate = DateUtilities.currentLocalDate();
        LocalTime currentTime = DateUtilities.currentLocalTime();
        OffsetDateTime nowLima = OffsetDateTime.now(LIMA_ZONE);

        // 1. Barrido de PENDIENTES
        List<BookingEntity> pendingBookings = bookingRepository.findByStatus(BookingStatus.PENDING.getValue());
        for (BookingEntity booking : pendingBookings) {
            processPendingBooking(booking, nowLima);
        }

        // 2. Barrido de CONFIRMADAS
        List<BookingEntity> confirmedBookings = bookingRepository.findByStatus(BookingStatus.CONFIRMED.getValue());
        for (BookingEntity booking : confirmedBookings) {
            processConfirmedBooking(booking, nowLima);
        }
    }

    private void processPendingBooking(BookingEntity booking, OffsetDateTime nowLima) {
        OffsetDateTime startDateTime = getFirstSlotStartDateTime(booking);
        if (startDateTime == null) return;
        
        long minutesUntilStart = Duration.between(nowLima, startDateTime).toMinutes();

        // Evitar procesar si la reserva es muy lejana (mas de 24 hrs por ej)
        if (minutesUntilStart > 1440 || minutesUntilStart < 0) {
            return;
        }

        Optional<VenueEntity> optVenue = venueRepository.findById(booking.getVenueId());
        if (optVenue.isEmpty()) return;
        VenueEntity venue = optVenue.get();

        if (minutesUntilStart <= hostAutoConfirmMinutes) {
            // Auto-Confirm Window
            boolean alreadyAutoConfirmed = notificationRepository.existsByRelatedIdAndType(booking.getId(), NotificationType.SYSTEM_AUTO_CONFIRMED);
            if (!alreadyAutoConfirmed) {
                log.info("[BookingNotificationScheduler] Auto-confirmando reserva {} al llegar a {} mins", booking.getId(), hostAutoConfirmMinutes);
                bookingService.confirmBySystem(booking.getId());
                
                // Notificar al Host
                String body = String.format("El sistema ha auto-confirmado la reserva **%s** de **%s** debido a proximidad de tiempo.", 
                        booking.getBookingCode(), venue.getName());
                
                notificationService.sendPushAndSave(
                    venue.getHostUserId(),
                    "Reserva Auto Confirmada",
                    body,
                    NotificationType.SYSTEM_AUTO_CONFIRMED,
                    booking.getId(),
                    Map.of("route", RouteConstants.HOST_RESERVATIONS_STANDALONE)
                );
            }
        } else if (minutesUntilStart <= hostReminderMinutes) {
            // Reminder Window
            boolean alreadyReminded = notificationRepository.existsByRelatedIdAndType(booking.getId(), NotificationType.HOST_PENDING_REMINDER);
            if (!alreadyReminded) {
                String dynamicTime = DateUtilities.formatMinutesToText(hostAutoConfirmMinutes); // Avisar en relacion al autoconfirm
                String body = String.format("El sistema confirmará automáticamente la reserva **%s** para la cancha **%s** en %s.", 
                        booking.getBookingCode(), venue.getName(), dynamicTime);
                
                notificationService.sendPushAndSave(
                    venue.getHostUserId(),
                    "¡Acción Requerida!",
                    body,
                    NotificationType.HOST_PENDING_REMINDER,
                    booking.getId(),
                    Map.of("route", RouteConstants.HOST_RESERVATIONS_STANDALONE)
                );
            }
        }
    }

    private void processConfirmedBooking(BookingEntity booking, OffsetDateTime nowLima) {
        OffsetDateTime startDateTime = getFirstSlotStartDateTime(booking);
        if (startDateTime == null) return;

        long minutesUntilStart = Duration.between(nowLima, startDateTime).toMinutes();

        if (minutesUntilStart > 1440 || minutesUntilStart < 0) {
            return;
        }

        if (minutesUntilStart <= clientReminderMinutes) {
            boolean alreadyReminded = notificationRepository.existsByRelatedIdAndType(booking.getId(), NotificationType.CLIENT_UPCOMING_REMINDER);
            if (!alreadyReminded) {
                Optional<VenueEntity> optVenue = venueRepository.findById(booking.getVenueId());
                if (optVenue.isEmpty()) return;
                VenueEntity venue = optVenue.get();

                String dynamicTime = DateUtilities.formatMinutesToText(clientReminderMinutes);
                String body = String.format("Tu reserva **%s** en **%s** empezará en aproximadamente %s.", 
                        booking.getBookingCode(), venue.getName(), dynamicTime);

                notificationService.sendPushAndSave(
                    booking.getClientUserId(),
                    "¡Tu turno está cerca!",
                    body,
                    NotificationType.CLIENT_UPCOMING_REMINDER,
                    booking.getId(),
                    Map.of("route", RouteConstants.RESERVATIONS_STANDALONE)
                );
            }
        }
    }

    private OffsetDateTime getFirstSlotStartDateTime(BookingEntity booking) {
        if (booking.getDetails() == null || booking.getDetails().isEmpty()) {
            return null;
        }
        
        List<BookingDetailEntity> sortedDetails = new java.util.ArrayList<>(booking.getDetails());
        sortedDetails.sort(java.util.Comparator.comparing(BookingDetailEntity::getStartTime));
        
        LocalTime firstStartTime = sortedDetails.get(0).getStartTime();
        
        // Convertir LocalDate + LocalTime de la reserva a OffsetDateTime en zona LIMA
        return OffsetDateTime.of(booking.getBookingDate(), firstStartTime, LIMA_ZONE.getRules().getOffset(java.time.Instant.now()));
    }
}
