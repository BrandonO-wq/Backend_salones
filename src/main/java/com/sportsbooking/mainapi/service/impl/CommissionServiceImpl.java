package com.sportsbooking.mainapi.service.impl;

import com.sportsbooking.mainapi.dto.commission.VenueCommissionResponse;
import com.sportsbooking.mainapi.jpa.entity.BookingEntity;
import com.sportsbooking.mainapi.jpa.entity.HostDebtResponseDto;
import com.sportsbooking.mainapi.jpa.entity.HostDebts;
import com.sportsbooking.mainapi.jpa.entity.ParameterEntity;
import com.sportsbooking.mainapi.jpa.entity.VenueEntity;
import com.sportsbooking.mainapi.jpa.entity.enums.BookingStatus;
import com.sportsbooking.mainapi.jpa.repository.BookingRepository;
import com.sportsbooking.mainapi.jpa.repository.HostDebtsRepository;
import com.sportsbooking.mainapi.jpa.repository.ParameterRepository;
import com.sportsbooking.mainapi.jpa.repository.VenueRepository;
import com.sportsbooking.mainapi.service.CommissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommissionServiceImpl implements CommissionService {

        private final HostDebtsRepository hostDebtsRepository;
        private final BookingRepository bookingRepository;
        private final VenueRepository venueRepository;
        private final ParameterRepository parameterRepository;

        @Value("${app.commission.cycle-days:15}")
        private int commissionCycleDays;

        @Value("${app.commission.deadline-offset-days:1}")
        private int deadlineOffsetDays;

        // Supongamos una comisión fija del 10% para el ejemplo
        private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.10");

        /**
         * Tarea programada para generar las deudas de comisiones de los anfitriones.
         * Se ejecuta a las 11:00 PM cada Domingo para procesar el ciclo que acaba de
         * terminar.
         * Cron: (segundo minuto hora día-del-mes mes día-de-la-semana)
         * 0 0 23 * * SUN = 23:00 (11 PM) cada domingo
         */
        @Scheduled(cron = "0 0 23 * * SUN")
        @Transactional
        public void generateDebtsForPreviousCycle() {
                // El ciclo que acaba de terminar finalizó hoy (Domingo)
                LocalDate cycleEnd = LocalDate.now();
                // El ciclo comenzó hace 14 días (13 días atrás, es decir el Lunes de hace 2
                // semanas)
                LocalDate cycleStart = cycleEnd.minusDays(13);

                log.info("[CommissionScheduler] Iniciando generación de deudas para el ciclo de 14 días: {} a {}",
                                cycleStart, cycleEnd);

                if (hostDebtsRepository.existsByCycleStart(cycleStart)) {
                        log.warn(
                                        "[CommissionScheduler] Las deudas para el ciclo que inicia en {} ya fueron generadas. Omitiendo ejecución.",
                                        cycleStart);
                        return;
                }

                // 1. Obtener todas las reservas completadas en el ciclo (usando createdAt)
                List<BookingEntity> completedBookings = bookingRepository.findByStatusAndCreatedAtDateBetween(
                                BookingStatus.COMPLETED.getValue(), cycleStart, cycleEnd);

                if (completedBookings.isEmpty()) {
                        log.info(
                                        "[CommissionScheduler] No se encontraron reservas completadas en el ciclo. No se generarán deudas.");
                        return;
                }

                // 2. Agrupar reservas por venue para calcular la deuda de cada uno
                Map<UUID, List<BookingEntity>> bookingsByVenue = completedBookings.stream()
                                .collect(Collectors.groupingBy(BookingEntity::getVenueId));

                // 3. Obtener la comisión del parámetro 57
                ParameterEntity commissionParam = parameterRepository.findById(57).orElseThrow(
                                () -> new IllegalArgumentException("Parameter 57 (commission rate) not found"));
                BigDecimal commissionRate = BigDecimal.valueOf(commissionParam.getValue());

                // 4. Obtener la información de los venues para saber a qué host pertenecen
                Set<UUID> venueIds = bookingsByVenue.keySet();
                Map<UUID, VenueEntity> venuesById = venueRepository.findAllById(venueIds).stream()
                                .collect(Collectors.toMap(VenueEntity::getId, Function.identity()));

                // 5. Calcular y guardar la deuda para cada venue
                bookingsByVenue.forEach((venueId, venueBookings) -> {
                        VenueEntity venue = venuesById.get(venueId);
                        if (venue == null) {
                                log.warn("[CommissionScheduler] No se encontró el venue con ID {}. Omitiendo deuda para este venue.",
                                                venueId);
                                return;
                        }

                        UUID hostId = venue.getHostUserId();

                        BigDecimal totalIncome = venueBookings.stream()
                                        .map(BookingEntity::getTotalAmount)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                        BigDecimal commissionAmount = totalIncome.multiply(commissionRate);

                        LocalDate deadlineDate = cycleEnd.plusDays(1);

                        HostDebts newDebt = HostDebts.builder()
                                        .hostUserId(hostId)
                                        .venueId(venueId)
                                        .cycleStart(cycleStart)
                                        .cycleEnd(cycleEnd)
                                        .deadline(deadlineDate.atStartOfDay().atOffset(ZoneOffset.UTC))
                                        .totalIncome(totalIncome)
                                        .commissionAmount(commissionAmount)
                                        .build(); // El estado PENDING es el default

                        hostDebtsRepository.save(newDebt);
                        log.info("[CommissionScheduler] Deuda generada para host {} y venue {} por un monto de {}",
                                        hostId, venueId,
                                        commissionAmount);
                });

                log.info("[CommissionScheduler] Finalizada la generación de deudas. Se procesaron {} venues.",
                                bookingsByVenue.size());
        }

        @Override
        public List<HostDebtResponseDto> getHostDebts(UUID hostId) {
                return hostDebtsRepository.findAll().stream()
                                .filter(debt -> debt.getHostUserId() != null && debt.getHostUserId().equals(hostId))
                                .sorted((a, b) -> b.getCreatedAt() == null ? -1
                                                : a.getCreatedAt() == null ? 1
                                                                : b.getCreatedAt().compareTo(a.getCreatedAt()))
                                .map(this::toDto)
                                .collect(Collectors.toList());
        }

        @Override
        public VenueCommissionResponse calculateCurrentCycleCommission(UUID venueId) {
                // Calcular el ciclo actual: 14 días empezando Lunes y terminando Domingo
                LocalDate today = LocalDate.now();
                LocalDate lastMonday = today.minusDays(today.getDayOfWeek().getValue() - 1); // Lunes de esta semana

                // Calcular si estamos en la primera o segunda semana del ciclo de 14 días
                long daysSinceEpoch = lastMonday.toEpochDay();
                long weekNumber = daysSinceEpoch / 7;

                LocalDate cycleStart;
                if (weekNumber % 2 == 0) {
                        // Primera semana del ciclo actual
                        cycleStart = lastMonday;
                } else {
                        // Segunda semana del ciclo actual, retroceder 7 días al Lunes anterior
                        cycleStart = lastMonday.minusDays(7);
                }

                LocalDate cycleEnd = cycleStart.plusDays(13); // 14 días (0-13)

                // Obtener comisión del parámetro 57
                ParameterEntity commissionParam = parameterRepository.findById(57).orElseThrow(
                                () -> new IllegalArgumentException("Parameter 57 (commission rate) not found"));
                BigDecimal commissionRate = BigDecimal.valueOf(commissionParam.getValue());

                // Obtener el nombre del salón
                VenueEntity venue = venueRepository.findById(venueId).orElseThrow(
                                () -> new IllegalArgumentException("Venue not found: " + venueId));

                // Obtener todas las reservas completadas del venue en el ciclo (usando
                // createdAt)
                List<BookingEntity> completedBookings = bookingRepository.findByStatusAndCreatedAtDateBetween(
                                BookingStatus.COMPLETED.getValue(), cycleStart, cycleEnd).stream()
                                .filter(b -> b.getVenueId().equals(venueId))
                                .collect(Collectors.toList());

                // Calcular ingresos totales
                BigDecimal totalIncome = completedBookings.stream()
                                .map(BookingEntity::getTotalAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Calcular comisión
                BigDecimal commissionAmount = totalIncome.multiply(commissionRate);

                // Deadline es un día después del fin de ciclo
                OffsetDateTime deadline = cycleEnd.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

                return VenueCommissionResponse.builder()
                                .venueName(venue.getName())
                                .cycleStart(cycleStart)
                                .cycleEnd(cycleEnd)
                                .totalIncome(totalIncome)
                                .commissionAmount(commissionAmount)
                                .deadline(deadline)
                                .commissionRate(commissionRate)
                                .build();
        }

        private HostDebtResponseDto toDto(HostDebts debt) {
                return HostDebtResponseDto.builder()
                                .id(debt.getId())
                                .hostUserId(debt.getHostUserId())
                                .venueId(debt.getVenueId())
                                .cycleStart(debt.getCycleStart())
                                .cycleEnd(debt.getCycleEnd())
                                .deadline(debt.getDeadline())
                                .totalIncome(debt.getTotalIncome())
                                .commissionAmount(debt.getCommissionAmount())
                                .paidAmount(debt.getPaidAmount())
                                .status(debt.getStatus())
                                .createdAt(debt.getCreatedAt())
                                .updatedAt(debt.getUpdatedAt())
                                .build();
        }
}
