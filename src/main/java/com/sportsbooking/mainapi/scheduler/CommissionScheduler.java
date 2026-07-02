package com.sportsbooking.mainapi.scheduler;

import com.sportsbooking.mainapi.jpa.entity.BookingEntity;
import com.sportsbooking.mainapi.jpa.entity.HostDebts;
import com.sportsbooking.mainapi.jpa.entity.ParameterEntity;
import com.sportsbooking.mainapi.jpa.entity.VenueEntity;
import com.sportsbooking.mainapi.jpa.entity.enums.HostDebtStatus;
import com.sportsbooking.mainapi.jpa.repository.BookingRepository;
import com.sportsbooking.mainapi.jpa.repository.HostDebtsRepository;
import com.sportsbooking.mainapi.jpa.repository.ParameterRepository;
import com.sportsbooking.mainapi.jpa.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommissionScheduler {

    private static final int COMMISSION_PARAMETER_ID = 57;
    private static final int PENDING_STATUS = 3;

    private final BookingRepository bookingRepository;
    private final HostDebtsRepository hostDebtsRepository;
    private final ParameterRepository parameterRepository;
    private final VenueRepository venueRepository;

    @Scheduled(cron = "0 0 0 * * MON")
    @Transactional
    public void generateWeeklyCommissions() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        Optional<ParameterEntity> parameterOpt = parameterRepository.findById(COMMISSION_PARAMETER_ID);
        if (parameterOpt.isEmpty()) {
            log.warn("No se encontró el parámetro de comisión con id {}", COMMISSION_PARAMETER_ID);
            return;
        }

        double commissionRate = Optional.ofNullable(parameterOpt.get().getValue()).orElse(0.0) / 100.0;
        List<BookingEntity> bookings = bookingRepository.findByStatus(PENDING_STATUS);

        for (BookingEntity booking : bookings) {
            if (booking.getTotalAmount() == null || booking.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (booking.getConfirmedAt() == null || booking.getConfirmedAt().toLocalDate().isBefore(weekStart)
                    || booking.getConfirmedAt().toLocalDate().isAfter(weekEnd)) {
                continue;
            }

            VenueEntity venue = venueRepository.findById(booking.getVenueId()).orElse(null);
            if (venue == null || venue.getHostUserId() == null) {
                continue;
            }

            BigDecimal commissionAmount = booking.getTotalAmount().multiply(BigDecimal.valueOf(commissionRate))
                    .setScale(2, RoundingMode.HALF_UP);

            HostDebts debt = HostDebts.builder()
                    .hostUserId(venue.getHostUserId())
                    .venueId(booking.getVenueId())
                    .cycleStart(weekStart)
                    .cycleEnd(weekEnd)
                    .deadline(OffsetDateTime.now().plusDays(7))
                    .totalIncome(booking.getTotalAmount())
                    .commissionAmount(commissionAmount)
                    .paidAmount(BigDecimal.ZERO)
                    .status(HostDebtStatus.PENDING)
                    .build();

            hostDebtsRepository.save(debt);
            log.info("Comisión semanal creada para venue {} con monto {}", booking.getVenueId(), commissionAmount);
        }
    }
}
