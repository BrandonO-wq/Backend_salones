package com.sportsbooking.mainapi.scheduler;

import com.sportsbooking.mainapi.common.utils.DateUtilities;
import com.sportsbooking.mainapi.jpa.entity.BookingDetailEntity;
import com.sportsbooking.mainapi.jpa.entity.BookingEntity;
import com.sportsbooking.mainapi.jpa.entity.enums.BookingStatus;
import com.sportsbooking.mainapi.jpa.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scheduler.booking.enabled", havingValue = "true", matchIfMissing = false)
public class BookingStatusScheduler {

    private final BookingRepository bookingRepository;

    @Scheduled(fixedDelayString = "${app.scheduler.booking.interval:60}", timeUnit = TimeUnit.SECONDS)
    @Transactional
    public void markConfirmedBookingsAsCompleted() {
        log.debug("[BookingStatusScheduler] Iniciando validación de reservas expiradas...");
        
        LocalDate currentDate = DateUtilities.currentLocalDate();
        LocalTime currentTime = DateUtilities.currentLocalTime();

        // Buscar reservas confirmadas
        List<BookingEntity> confirmedBookings = bookingRepository.findByStatus(BookingStatus.CONFIRMED.getValue());

        int count = 0;
        for (BookingEntity booking : confirmedBookings) {
            if (isCompleted(booking.getBookingDate(), booking.getDetails(), currentDate, currentTime)) {
                booking.setStatus(BookingStatus.COMPLETED.getValue());
                bookingRepository.save(booking);
                count++;
                log.info("[BookingStatusScheduler] Reserva {} marcada como COMPLETED", booking.getId());
            }
        }

        if (count > 0) {
            log.info("[BookingStatusScheduler] Finalizado. Se actualizaron {} reservas a COMPLETED.", count);
        }
    }

    private boolean isCompleted(LocalDate bookingDate, List<BookingDetailEntity> details, LocalDate currentDate, LocalTime currentTime) {
        if (bookingDate.isBefore(currentDate)) {
            return true;
        } else if (bookingDate.isEqual(currentDate) && details != null && !details.isEmpty()) {
            List<BookingDetailEntity> sortedDetails = new java.util.ArrayList<>(details);
            sortedDetails.sort(java.util.Comparator.comparing(BookingDetailEntity::getStartTime));
            
            BookingDetailEntity last = sortedDetails.get(sortedDetails.size() - 1);
            if (last.getEndTime().isBefore(currentTime)) {
                return true;
            }
        }
        return false;
    }
}
