package com.sportsbooking.mainapi.service.impl;

import com.sportsbooking.mainapi.dto.commission.HostDebtorResponseDto;
import com.sportsbooking.mainapi.dto.commission.VenueCommissionResponse;
import com.sportsbooking.mainapi.dto.commission.RegisteredHostResponseDto;
import com.sportsbooking.mainapi.dto.commission.VenueDebtReportResponseDto;
import com.sportsbooking.mainapi.jpa.repository.VenueReviewRepository;
import com.sportsbooking.mainapi.jpa.entity.VenueReviewEntity;
import com.sportsbooking.mainapi.jpa.entity.BookingEntity;
import com.sportsbooking.mainapi.jpa.entity.HostDebtResponseDto;
import com.sportsbooking.mainapi.jpa.entity.HostDebts;
import com.sportsbooking.mainapi.jpa.entity.ParameterEntity;
import com.sportsbooking.mainapi.jpa.entity.UserEntity;
import com.sportsbooking.mainapi.jpa.entity.VenueEntity;
import com.sportsbooking.mainapi.jpa.entity.enums.BookingStatus;
import com.sportsbooking.mainapi.jpa.entity.enums.HostDebtStatus;
import com.sportsbooking.mainapi.jpa.entity.enums.VenueStatus;
import com.sportsbooking.mainapi.jpa.repository.BookingRepository;
import com.sportsbooking.mainapi.jpa.repository.HostDebtsRepository;
import com.sportsbooking.mainapi.jpa.repository.ParameterRepository;
import com.sportsbooking.mainapi.jpa.repository.UserRepository;
import com.sportsbooking.mainapi.jpa.repository.VenueRepository;
import com.sportsbooking.mainapi.service.CommissionService;
import com.sportsbooking.mainapi.components.OtpComponent;
import com.sportsbooking.mainapi.components.email.CommissionEmail;
import com.sportsbooking.mainapi.components.email.CycleEmail;
import com.sportsbooking.mainapi.dto.commission.ConfirmCycleConfigChangeRequest;
import com.sportsbooking.mainapi.dto.common.MessageResponse;
import com.sportsbooking.mainapi.common.exception.BusinessException;
import com.sportsbooking.mainapi.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
        private final UserRepository userRepository;
        private final OtpComponent otpComponent;
        private final CommissionEmail commissionEmail;
        private final CycleEmail cycleEmail;
        private final VenueReviewRepository venueReviewRepository;

        private static final int PARAM_CYCLE_DAYS = 58;
        private static final int PARAM_DEADLINE_OFFSET = 59;
        private static final int PARAM_CYCLE_START_DAY = 60;
        private static final int PARAM_CYCLE_START_HOUR = 61;

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
                int cycleDays = parameterRepository.findById(PARAM_CYCLE_DAYS)
                                .map(p -> p.getValue() != null ? p.getValue().intValue() : 14)
                                .orElse(14);
                int deadlineOffset = parameterRepository.findById(PARAM_DEADLINE_OFFSET)
                                .map(p -> p.getValue() != null ? p.getValue().intValue() : 1)
                                .orElse(1);
                int startHour = parameterRepository.findById(PARAM_CYCLE_START_HOUR)
                                .map(p -> p.getValue() != null ? p.getValue().intValue() : 0)
                                .orElse(0);

                // El ciclo que acaba de terminar finalizó hoy (Domingo)
                LocalDate cycleEnd = LocalDate.now();
                // El ciclo comenzó hace cycleDays - 1 días atrás
                LocalDate cycleStart = cycleEnd.minusDays(cycleDays - 1);

                OffsetDateTime startDateTime = cycleStart.atTime(java.time.LocalTime.of(startHour, 0)).atOffset(ZoneOffset.UTC);
                OffsetDateTime endDateTime = cycleEnd.plusDays(1).atTime(java.time.LocalTime.of(startHour, 0)).minusNanos(1).atOffset(ZoneOffset.UTC);

                log.info("[CommissionScheduler] Iniciando generación de deudas para el ciclo de {} días: {} a {}",
                                cycleDays, startDateTime, endDateTime);

                if (hostDebtsRepository.existsByCycleStart(cycleStart)) {
                        log.warn(
                                        "[CommissionScheduler] Las deudas para el ciclo que inicia en {} ya fueron generadas. Omitiendo ejecución.",
                                        cycleStart);
                        return;
                }

                // 1. Obtener todas las reservas confirmadas o completadas en el ciclo
                List<BookingEntity> completedBookings = bookingRepository.findByStatusInAndConfirmedAtBetween(
                                List.of(BookingStatus.CONFIRMED.getValue(), BookingStatus.COMPLETED.getValue()),
                                startDateTime, endDateTime);

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

                        LocalDate deadlineDate = cycleEnd.plusDays(deadlineOffset);

                        HostDebts newDebt = HostDebts.builder()
                                        .hostUserId(hostId)
                                        .venueId(venueId)
                                        .cycleStart(cycleStart)
                                        .cycleEnd(cycleEnd)
                                        .deadline(deadlineDate.atTime(java.time.LocalTime.of(startHour, 0)).atOffset(ZoneOffset.UTC))
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
                                .filter(debt -> hostId == null || (debt.getHostUserId() != null && debt.getHostUserId().equals(hostId)))
                                .sorted((a, b) -> b.getCreatedAt() == null ? -1
                                                 : a.getCreatedAt() == null ? 1
                                                                 : b.getCreatedAt().compareTo(a.getCreatedAt()))
                                .map(this::toDto)
                                .collect(Collectors.toList());
        }

        @Override
        public VenueCommissionResponse calculateCurrentCycleCommission(UUID venueId) {
                int cycleDays = parameterRepository.findById(PARAM_CYCLE_DAYS)
                                .map(p -> p.getValue() != null ? p.getValue().intValue() : 14)
                                .orElse(14);
                int deadlineOffset = parameterRepository.findById(PARAM_DEADLINE_OFFSET)
                                .map(p -> p.getValue() != null ? p.getValue().intValue() : 1)
                                .orElse(1);
                int startDayValue = parameterRepository.findById(PARAM_CYCLE_START_DAY)
                                .map(p -> p.getValue() != null ? p.getValue().intValue() : 1)
                                .orElse(1); // 1 = Lunes
                int startHour = parameterRepository.findById(PARAM_CYCLE_START_HOUR)
                                .map(p -> p.getValue() != null ? p.getValue().intValue() : 0)
                                .orElse(0);

                // Calcular el ciclo actual
                LocalDate today = LocalDate.now();
                int todayValue = today.getDayOfWeek().getValue();
                int daysToSubtract = (todayValue - startDayValue + 7) % 7;
                LocalDate mostRecentStartDay = today.minusDays(daysToSubtract);

                LocalDate cycleStart;
                if (cycleDays <= 7) {
                        cycleStart = mostRecentStartDay;
                } else {
                        // Calcular si estamos en la primera o segunda semana del ciclo de 14 días
                        long daysSinceEpoch = mostRecentStartDay.toEpochDay();
                        long weekNumber = daysSinceEpoch / 7;
                        if (weekNumber % 2 == 0) {
                                cycleStart = mostRecentStartDay;
                        } else {
                                cycleStart = mostRecentStartDay.minusDays(7);
                        }
                }

                LocalDate cycleEnd = cycleStart.plusDays(cycleDays - 1);

                OffsetDateTime startDateTime = cycleStart.atTime(java.time.LocalTime.of(startHour, 0)).atOffset(ZoneOffset.UTC);
                OffsetDateTime endDateTime = cycleEnd.plusDays(1).atTime(java.time.LocalTime.of(startHour, 0)).minusNanos(1).atOffset(ZoneOffset.UTC);

                // Obtener comisión del parámetro 57
                ParameterEntity commissionParam = parameterRepository.findById(57).orElseThrow(
                                () -> new IllegalArgumentException("Parameter 57 (commission rate) not found"));
                BigDecimal commissionRate = BigDecimal.valueOf(commissionParam.getValue());

                // Obtener el nombre del salón
                VenueEntity venue = venueRepository.findById(venueId).orElseThrow(
                                () -> new IllegalArgumentException("Venue not found: " + venueId));

                // Obtener todas las reservas confirmadas o completadas del venue en el ciclo (usando confirmedAt)
                List<BookingEntity> completedBookings = bookingRepository.findByStatusInAndConfirmedAtBetween(
                                List.of(BookingStatus.CONFIRMED.getValue(), BookingStatus.COMPLETED.getValue()),
                                startDateTime, endDateTime).stream()
                                .filter(b -> b.getVenueId().equals(venueId))
                                .collect(Collectors.toList());

                // Calcular ingresos totales
                BigDecimal totalIncome = completedBookings.stream()
                                .map(BookingEntity::getTotalAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Calcular comisión
                BigDecimal commissionAmount = totalIncome.multiply(commissionRate);

                // Deadline es deadlineOffset días después del fin de ciclo, a la hora de inicio especificada
                OffsetDateTime deadline = cycleEnd.plusDays(deadlineOffset).atTime(java.time.LocalTime.of(startHour, 0)).atOffset(ZoneOffset.UTC);

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

        @Override
        public List<HostDebtorResponseDto> getHostDebtors() {
                // 1. Obtener todas las deudas que no estén pagadas (status != PAID)
                List<HostDebts> unpaidDebts = hostDebtsRepository.findByStatusNot(HostDebtStatus.PAID);

                // 2. Agrupar deudas por hostUserId
                Map<UUID, List<HostDebts>> debtsByHost = unpaidDebts.stream()
                                .filter(debt -> debt.getHostUserId() != null)
                                .collect(Collectors.groupingBy(HostDebts::getHostUserId));

                OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

                // 3. Mapear cada host a su DTO correspondiente
                return debtsByHost.entrySet().stream()
                                .map(entry -> {
                                        UUID hostUserId = entry.getKey();
                                        List<HostDebts> hostDebts = entry.getValue();

                                        // Obtener información del usuario propietario
                                        UserEntity user = userRepository.findById(hostUserId).orElse(null);
                                        String hostName = user != null ? user.getName() : "Desconocido";
                                        String email = user != null ? user.getEmail() : "";

                                        // Número de canchas que posee
                                        long venuesCount = venueRepository.countByHostUserId(hostUserId);

                                        // Sumar cantidad de ingresos de las deudas pendientes
                                        BigDecimal totalIncome = hostDebts.stream()
                                                        .map(HostDebts::getTotalIncome)
                                                        .filter(income -> income != null)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                        // Sumar comisión pendiente
                                        BigDecimal pendingCommission = hostDebts.stream()
                                                        .map(HostDebts::getCommissionAmount)
                                                        .filter(commission -> commission != null)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                        // Calcular días de retraso del propietario (máximo retraso de sus deudas vencidas)
                                        long maxDelayDays = hostDebts.stream()
                                                        .mapToLong(debt -> {
                                                                if (debt.getDeadline() != null && debt.getDeadline().isBefore(now)) {
                                                                        return java.time.temporal.ChronoUnit.DAYS.between(debt.getDeadline(), now);
                                                                }
                                                                return 0L;
                                                        })
                                                        .max()
                                                        .orElse(0L);

                                        return HostDebtorResponseDto.builder()
                                                        .hostName(hostName)
                                                        .email(email)
                                                        .venuesCount(venuesCount)
                                                        .totalIncome(totalIncome)
                                                        .delayDays(maxDelayDays)
                                                        .pendingCommission(pendingCommission)
                                                        .build();
                                })
                                .collect(Collectors.toList());
        }

        @Override
        public List<RegisteredHostResponseDto> getRegisteredHosts() {
                List<UserEntity> hosts = userRepository.findHosts();

                if (hosts.isEmpty()) {
                        return List.of();
                }

                List<VenueEntity> allVenues = venueRepository.findAll();
                Map<UUID, List<VenueEntity>> venuesByHost = allVenues.stream()
                                .filter(v -> v.getHostUserId() != null)
                                .collect(Collectors.groupingBy(VenueEntity::getHostUserId));

                List<HostDebts> allDebts = hostDebtsRepository.findAll();
                Map<UUID, List<HostDebts>> debtsByHost = allDebts.stream()
                                .filter(d -> d.getHostUserId() != null)
                                .collect(Collectors.groupingBy(HostDebts::getHostUserId));

                return hosts.stream()
                                .map(host -> {
                                        UUID hostId = host.getId();
                                        List<VenueEntity> hostVenues = venuesByHost.getOrDefault(hostId, List.of());
                                        List<HostDebts> hostDebts = debtsByHost.getOrDefault(hostId, List.of());

                                        BigDecimal totalIncomeLastPeriod = BigDecimal.ZERO;
                                        BigDecimal commissionLastPeriod = BigDecimal.ZERO;
                                        OffsetDateTime deadlineLastPeriod = null;
                                        HostDebtStatus statusLastPeriod = null;

                                        if (!hostDebts.isEmpty()) {
                                                LocalDate maxCycleEnd = hostDebts.stream()
                                                                .map(HostDebts::getCycleEnd)
                                                                .max(LocalDate::compareTo)
                                                                .orElse(null);

                                                if (maxCycleEnd != null) {
                                                        List<HostDebts> latestPeriodDebts = hostDebts.stream()
                                                                        .filter(d -> d.getCycleEnd().equals(maxCycleEnd))
                                                                        .collect(Collectors.toList());

                                                        totalIncomeLastPeriod = latestPeriodDebts.stream()
                                                                        .map(HostDebts::getTotalIncome)
                                                                        .filter(income -> income != null)
                                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                                        commissionLastPeriod = latestPeriodDebts.stream()
                                                                        .map(HostDebts::getCommissionAmount)
                                                                        .filter(comm -> comm != null)
                                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                                        deadlineLastPeriod = latestPeriodDebts.stream()
                                                                        .map(HostDebts::getDeadline)
                                                                        .filter(dl -> dl != null)
                                                                        .findFirst()
                                                                        .orElse(null);

                                                        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
                                                        boolean hasOverdue = latestPeriodDebts.stream().anyMatch(d -> 
                                                                d.getStatus() == HostDebtStatus.OVERDUE || 
                                                                ((d.getStatus() == HostDebtStatus.PENDING || d.getStatus() == HostDebtStatus.LEGACY_PENDING) && 
                                                                 d.getDeadline() != null && d.getDeadline().isBefore(now))
                                                        );
                                                        boolean hasPending = latestPeriodDebts.stream().anyMatch(d -> 
                                                                d.getStatus() == HostDebtStatus.PENDING || d.getStatus() == HostDebtStatus.LEGACY_PENDING
                                                        );

                                                        if (hasOverdue) {
                                                                statusLastPeriod = HostDebtStatus.OVERDUE;
                                                        } else if (hasPending) {
                                                                statusLastPeriod = HostDebtStatus.PENDING;
                                                        } else {
                                                                statusLastPeriod = HostDebtStatus.PAID;
                                                        }
                                                }
                                        }

                                        return RegisteredHostResponseDto.builder()
                                                        .hostId(hostId)
                                                        .hostName(host.getName())
                                                        .registrationDate(host.getCreatedAt())
                                                        .email(host.getEmail())
                                                        .phone(host.getPhone())
                                                        .venuesCount(hostVenues.size())
                                                        .totalIncomeLastPeriod(totalIncomeLastPeriod)
                                                        .commissionLastPeriod(commissionLastPeriod)
                                                        .deadlineLastPeriod(deadlineLastPeriod)
                                                        .statusLastPeriod(statusLastPeriod)
                                                        .build();
                                })
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional
        public MessageResponse requestCommissionChange(UUID adminUserId) {
                UserEntity admin = userRepository.findById(adminUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("Usuario", adminUserId));

                // 1. Rate-limit global de OTP por email
                otpComponent.checkAndIncrementOtpRate(admin.getEmail());

                // 2. Verificar cooldown
                if (otpComponent.isCooldownActive(adminUserId)) {
                        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                                        "Debes esperar antes de volver a solicitar el código OTP.");
                }

                // 3. Generar OTP de 4 dígitos
                String otpCode = otpComponent.generateAndStoreOtp(adminUserId);

                // 4. Enviar email
                commissionEmail.send(admin.getName(), admin.getEmail(), otpCode);

                log.info("OTP de cambio de comisión enviado a admin: {} (id={})", admin.getEmail(), admin.getId());
                return new MessageResponse("Código de verificación enviado exitosamente a tu correo.");
        }

        @Override
        @Transactional
        public MessageResponse confirmCommissionChange(UUID adminUserId, String otpCode, Double newValue) {
                if (newValue == null || newValue < 0) {
                        throw new BusinessException("El valor de comisión debe ser mayor o igual a cero.");
                }

                // 1. Validar OTP
                boolean isValid = otpComponent.validateOtp(adminUserId, otpCode);
                if (!isValid) {
                        throw new BusinessException("El código de verificación es inválido o ha expirado.");
                }

                // 2. Buscar y actualizar el parámetro 57
                ParameterEntity commissionParam = parameterRepository.findById(57)
                                .orElseThrow(() -> new ResourceNotFoundException("Parámetro", 57));

                commissionParam.setValue(newValue);
                parameterRepository.save(commissionParam);

                log.info("Comisión de sistema actualizada a {} por admin ID: {}", newValue, adminUserId);
                return new MessageResponse("Comisión actualizada exitosamente.");
        }

        @Override
        @Transactional(readOnly = true)
        public Double getCommissionPercentage() {
                ParameterEntity commissionParam = parameterRepository.findById(57)
                                .orElseThrow(() -> new ResourceNotFoundException("Parámetro", 57));
                return commissionParam.getValue();
        }

        @Override
        @Transactional(readOnly = true)
        public List<VenueDebtReportResponseDto> getVenueDebtReport() {
                List<VenueEntity> venues = venueRepository.findAll();
                if (venues.isEmpty()) {
                        return List.of();
                }

                // 1. Obtener todos los usuarios propietarios para mapear en memoria
                Map<UUID, UserEntity> usersMap = userRepository.findAll().stream()
                                .filter(u -> u.getId() != null)
                                .collect(Collectors.toMap(UserEntity::getId, Function.identity(), (u1, u2) -> u1));

                // 2. Obtener todas las deudas y agruparlas por venueId en memoria
                List<HostDebts> allDebts = hostDebtsRepository.findAll();
                Map<UUID, List<HostDebts>> debtsByVenue = allDebts.stream()
                                .filter(d -> d.getVenueId() != null)
                                .collect(Collectors.groupingBy(HostDebts::getVenueId));

                // 3. Obtener todas las calificaciones y agrupar promedio en memoria
                List<VenueReviewEntity> allReviews = venueReviewRepository.findAll();
                Map<UUID, Double> ratingsByVenue = allReviews.stream()
                                .filter(r -> r.getVenueId() != null)
                                .collect(Collectors.groupingBy(
                                                VenueReviewEntity::getVenueId,
                                                Collectors.averagingDouble(VenueReviewEntity::getPoints)
                                ));

                OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

                return venues.stream().map(venue -> {
                        UserEntity owner = usersMap.get(venue.getHostUserId());
                        String ownerName = owner != null ? owner.getName() : "Sin propietario";
                        String ownerEmail = owner != null ? owner.getEmail() : "";

                        List<HostDebts> venueDebts = debtsByVenue.getOrDefault(venue.getId(), List.of());
                        List<HostDebts> unpaidDebts = venueDebts.stream()
                                        .filter(d -> d.getStatus() != HostDebtStatus.PAID)
                                        .collect(Collectors.toList());

                        String debtStatus = "PAID";
                        long delayDays = 0;

                        if (venueDebts.isEmpty()) {
                                debtStatus = "NONE";
                        } else if (!unpaidDebts.isEmpty()) {
                                boolean hasOverdue = unpaidDebts.stream().anyMatch(d -> 
                                        d.getStatus() == HostDebtStatus.OVERDUE || 
                                        (d.getDeadline() != null && d.getDeadline().isBefore(now))
                                );
                                debtStatus = hasOverdue ? "OVERDUE" : "PENDING";

                                delayDays = unpaidDebts.stream()
                                                .mapToLong(d -> {
                                                        if (d.getDeadline() != null && d.getDeadline().isBefore(now)) {
                                                                return java.time.temporal.ChronoUnit.DAYS.between(d.getDeadline(), now);
                                                        }
                                                        return 0L;
                                                })
                                                .max()
                                                .orElse(0L);
                        }

                        boolean isActive = venue.getStatus() == VenueStatus.ACTIVE;

                        double commissionAmount = 0.0;
                        double totalIncome = 0.0;
                        OffsetDateTime deadline = null;

                        if (isActive) {
                                VenueCommissionResponse currentCommission = calculateCurrentCycleCommission(venue.getId());
                                if (currentCommission != null) {
                                        commissionAmount = currentCommission.getCommissionAmount() != null 
                                                        ? currentCommission.getCommissionAmount().doubleValue() : 0.0;
                                        totalIncome = currentCommission.getTotalIncome() != null 
                                                        ? currentCommission.getTotalIncome().doubleValue() : 0.0;
                                        deadline = currentCommission.getDeadline();
                                }
                        } else {
                                if (!unpaidDebts.isEmpty()) {
                                        commissionAmount = unpaidDebts.stream()
                                                        .mapToDouble(d -> d.getCommissionAmount() != null ? d.getCommissionAmount().doubleValue() : 0.0)
                                                        .sum();
                                        totalIncome = unpaidDebts.stream()
                                                        .mapToDouble(d -> d.getTotalIncome() != null ? d.getTotalIncome().doubleValue() : 0.0)
                                                        .sum();
                                        deadline = unpaidDebts.stream()
                                                        .map(HostDebts::getDeadline)
                                                        .filter(d -> d != null)
                                                        .min(OffsetDateTime::compareTo)
                                                        .orElse(null);
                                } else if (!venueDebts.isEmpty()) {
                                        HostDebts latestDebt = venueDebts.stream()
                                                        .max((d1, d2) -> {
                                                                if (d1.getCycleEnd() == null || d2.getCycleEnd() == null) return 0;
                                                                return d1.getCycleEnd().compareTo(d2.getCycleEnd());
                                                        }).orElse(null);
                                        if (latestDebt != null) {
                                                commissionAmount = latestDebt.getCommissionAmount() != null ? latestDebt.getCommissionAmount().doubleValue() : 0.0;
                                                totalIncome = latestDebt.getTotalIncome() != null ? latestDebt.getTotalIncome().doubleValue() : 0.0;
                                                deadline = latestDebt.getDeadline();
                                        }
                                }
                        }

                        double rating = ratingsByVenue.getOrDefault(venue.getId(), 0.0);
                        double latitude = venue.getLocation() != null ? venue.getLocation().getY() : 0.0;
                        double longitude = venue.getLocation() != null ? venue.getLocation().getX() : 0.0;

                        return VenueDebtReportResponseDto.builder()
                                        .venueId(venue.getId())
                                        .venueName(venue.getName())
                                        .ownerName(ownerName)
                                        .ownerEmail(ownerEmail)
                                        .delayDays(delayDays)
                                        .address(venue.getAddress())
                                        .latitude(latitude)
                                        .longitude(longitude)
                                        .ubigeo(venue.getUbigeo())
                                        .rating(rating)
                                        .debtStatus(debtStatus)
                                        .commissionAmount(commissionAmount)
                                        .totalIncome(totalIncome)
                                        .deadline(deadline)
                                        .build();
                }).collect(Collectors.toList());
        }

        @Override
        @org.springframework.transaction.annotation.Transactional
        public MessageResponse toggleVenueStatus(UUID venueId, boolean active) {
                VenueEntity venue = venueRepository.findById(venueId)
                                .orElseThrow(() -> new ResourceNotFoundException("Venue", venueId));
                if (active) {
                        venue.setStatus(com.sportsbooking.mainapi.jpa.entity.enums.VenueStatus.ACTIVE);
                        venue.setDisabledReason(3);
                } else {
                        venue.setStatus(com.sportsbooking.mainapi.jpa.entity.enums.VenueStatus.INACTIVE);
                        venue.setDisabledReason(2);
                }
                venueRepository.save(venue);
                return new MessageResponse("Estado del salón actualizado con éxito");
        }

        @Override
        @org.springframework.transaction.annotation.Transactional
        public MessageResponse enableVenue(UUID venueId) {
                VenueEntity venue = venueRepository.findById(venueId)
                                .orElseThrow(() -> new ResourceNotFoundException("Venue", venueId));
                venue.setStatus(com.sportsbooking.mainapi.jpa.entity.enums.VenueStatus.ACTIVE);
                venue.setDisabledReason(3);
                venueRepository.save(venue);
                return new MessageResponse("Salón habilitado con éxito");
        }

        @Override
        @org.springframework.transaction.annotation.Transactional(readOnly = true)
        public Map<String, Object> getVenueDisabledStatus(UUID venueId) {
                VenueEntity venue = venueRepository.findById(venueId)
                                .orElseThrow(() -> new ResourceNotFoundException("Venue", venueId));
                
                Integer reason = venue.getDisabledReason();
                if (reason == null) {
                        reason = 0;
                }
                
                String description;
                switch (reason) {
                        case 1:
                                description = "bloqueado automatico";
                                break;
                        case 2:
                                description = "bloqueado manualmente";
                                break;
                        case 3:
                                description = "habilitado manualmente";
                                break;
                        case 0:
                        default:
                                description = "activo";
                                break;
                }
                
                return Map.of(
                        "disabledReason", reason,
                        "description", description,
                        "status", venue.getStatus() != null ? venue.getStatus().name() : "UNKNOWN"
                );
        }

        @Override
        @org.springframework.transaction.annotation.Transactional(readOnly = true)
        public Map<String, Object> getCycleConfig() {
                int cycleDays = parameterRepository.findById(PARAM_CYCLE_DAYS)
                                .map(p -> p.getValue() != null ? p.getValue().intValue() : 14)
                                .orElse(14);
                int deadlineOffset = parameterRepository.findById(PARAM_DEADLINE_OFFSET)
                                .map(p -> p.getValue() != null ? p.getValue().intValue() : 1)
                                .orElse(1);
                int startDayValue = parameterRepository.findById(PARAM_CYCLE_START_DAY)
                                .map(p -> p.getValue() != null ? p.getValue().intValue() : 1)
                                .orElse(1);
                int startHour = parameterRepository.findById(PARAM_CYCLE_START_HOUR)
                                .map(p -> p.getValue() != null ? p.getValue().intValue() : 0)
                                .orElse(0);

                String startDayName;
                switch (startDayValue) {
                        case 1: startDayName = "Lunes"; break;
                        case 2: startDayName = "Martes"; break;
                        case 3: startDayName = "Miércoles"; break;
                        case 4: startDayName = "Jueves"; break;
                        case 5: startDayName = "Viernes"; break;
                        case 6: startDayName = "Sábado"; break;
                        case 7: startDayName = "Domingo"; break;
                        default: startDayName = "Desconocido"; break;
                }

                return Map.of(
                        "cycleDays", cycleDays,
                        "deadlineOffsetDays", deadlineOffset,
                        "startDayOfWeek", startDayValue,
                        "startDayName", startDayName,
                        "startHour", startHour
                );
        }

        @Override
        @Transactional
        public MessageResponse requestCycleConfigChange(UUID adminUserId) {
                UserEntity admin = userRepository.findById(adminUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("Usuario", adminUserId));

                // 1. Rate-limit de OTP
                otpComponent.checkAndIncrementOtpRate(admin.getEmail());

                // 2. Cooldown check
                if (otpComponent.isCooldownActive(adminUserId)) {
                        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                                        "Debes esperar antes de volver a solicitar el código OTP.");
                }

                // 3. Generar OTP
                String otpCode = otpComponent.generateAndStoreOtp(adminUserId);

                // 4. Enviar email
                cycleEmail.send(admin.getName(), admin.getEmail(), otpCode);

                log.info("OTP de cambio de ciclo enviado a admin: {} (id={})", admin.getEmail(), admin.getId());
                return new MessageResponse("Código de verificación enviado exitosamente a tu correo.");
        }

        @Override
        @Transactional
        public MessageResponse confirmCycleConfigChange(UUID adminUserId, ConfirmCycleConfigChangeRequest request) {
                // 1. Validar OTP
                boolean isValid = otpComponent.validateOtp(adminUserId, request.otpCode());
                if (!isValid) {
                        throw new BusinessException("El código de verificación es inválido o ha expirado.");
                }

                // 2. Buscar y actualizar los parámetros 58, 59, 60, 61
                ParameterEntity cycleDaysParam = parameterRepository.findById(PARAM_CYCLE_DAYS)
                                .orElseThrow(() -> new ResourceNotFoundException("Parámetro", PARAM_CYCLE_DAYS));
                ParameterEntity deadlineOffsetParam = parameterRepository.findById(PARAM_DEADLINE_OFFSET)
                                .orElseThrow(() -> new ResourceNotFoundException("Parámetro", PARAM_DEADLINE_OFFSET));
                ParameterEntity startDayParam = parameterRepository.findById(PARAM_CYCLE_START_DAY)
                                .orElseThrow(() -> new ResourceNotFoundException("Parámetro", PARAM_CYCLE_START_DAY));
                ParameterEntity startHourParam = parameterRepository.findById(PARAM_CYCLE_START_HOUR)
                                .orElseThrow(() -> new ResourceNotFoundException("Parámetro", PARAM_CYCLE_START_HOUR));

                cycleDaysParam.setValue(request.cycleDays().doubleValue());
                deadlineOffsetParam.setValue(request.deadlineOffsetDays().doubleValue());
                startDayParam.setValue(request.startDayOfWeek().doubleValue());
                startHourParam.setValue(request.startHour().doubleValue());

                parameterRepository.save(cycleDaysParam);
                parameterRepository.save(deadlineOffsetParam);
                parameterRepository.save(startDayParam);
                parameterRepository.save(startHourParam);

                log.info("Configuración de ciclo de comisiones actualizada: ciclo={}, límite={}, día={}, hora={} por admin ID: {}",
                                request.cycleDays(), request.deadlineOffsetDays(), request.startDayOfWeek(), request.startHour(), adminUserId);
                return new MessageResponse("Configuración del ciclo de comisiones actualizada exitosamente.");
        }
}
