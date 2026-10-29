package com.sportsbooking.mainapi.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbooking.mainapi.common.exception.BusinessException;
import com.sportsbooking.mainapi.common.exception.ResourceNotFoundException;
import com.sportsbooking.mainapi.common.utils.DateUtilities;
import com.sportsbooking.mainapi.common.components.BookingCodeGenerator;
import com.sportsbooking.mainapi.service.UbigeoService;
import com.sportsbooking.mainapi.dto.booking.request.HoldSlotsRequest;
import com.sportsbooking.mainapi.dto.booking.request.SlotItem;
import com.sportsbooking.mainapi.dto.booking.response.BookingResponse;
import com.sportsbooking.mainapi.dto.booking.response.HoldResponse;
import com.sportsbooking.mainapi.dto.booking.response.HostBookingResponse;
import com.sportsbooking.mainapi.dto.booking.response.SlotAvailability;
import com.sportsbooking.mainapi.dto.booking.response.*;
import com.sportsbooking.mainapi.common.constants.RouteConstants;
import com.sportsbooking.mainapi.jpa.entity.AvailableVenueEntity;
import com.sportsbooking.mainapi.jpa.entity.BookingDetailEntity;
import com.sportsbooking.mainapi.jpa.entity.VenuePhotoEntity;
import com.sportsbooking.mainapi.jpa.entity.enums.NotificationType;
import com.sportsbooking.mainapi.jpa.entity.enums.ScheduleType;
import com.sportsbooking.mainapi.jpa.entity.enums.VenueStatus;
import com.sportsbooking.mainapi.jpa.repository.VenuePhotoRepository;
import com.sportsbooking.mainapi.jpa.entity.BookingEntity;
import com.sportsbooking.mainapi.jpa.entity.UserEntity;
import com.sportsbooking.mainapi.jpa.entity.VenueEntity;
import com.sportsbooking.mainapi.jpa.entity.enums.BookingStatus;
import com.sportsbooking.mainapi.jpa.repository.AvailableVenueRepository;
import com.sportsbooking.mainapi.jpa.repository.BookingDetailRepository;
import com.sportsbooking.mainapi.jpa.repository.BookingRepository;
import com.sportsbooking.mainapi.jpa.repository.UserRepository;
import com.sportsbooking.mainapi.jpa.repository.VenueRepository;
import com.sportsbooking.mainapi.jpa.repository.ParameterRepository;
import com.sportsbooking.mainapi.jpa.entity.ParameterEntity;
import com.sportsbooking.mainapi.jpa.repository.VenueReviewRepository;
import com.sportsbooking.mainapi.jpa.entity.VenueReviewEntity;
import com.sportsbooking.mainapi.service.BookingService;
import com.sportsbooking.mainapi.service.FileStorageService;
import com.sportsbooking.mainapi.service.NotificationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final AvailableVenueRepository availableVenueRepository;
    private final FileStorageService fileStorageService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final VenuePhotoRepository venuePhotoRepository;
    private final VenueReviewRepository venueReviewRepository;
    private final com.sportsbooking.mainapi.service.ParameterService parameterService;
    private final com.sportsbooking.mainapi.service.ReviewService reviewService;
    private final UbigeoService ubigeoService;
    private final ParameterRepository parameterRepository;
    private final BookingCodeGenerator bookingCodeGenerator;
    private final com.sportsbooking.mainapi.service.CommissionService commissionService;

    @jakarta.annotation.PostConstruct
    public void setupJackson() {
        objectMapper.findAndRegisterModules();
    }

    @Value("${app.booking.slot-buffer-minutes:30}")
    private int slotBufferMinutes;

    @Value("${app.booking.hold-ttl-minutes:10}")
    private int baseHoldTtlMinutes;

    @Value("${app.booking.hold-ttl-buffer-seconds:30}")
    private int holdTtlBufferSeconds;

    private static final String SLOT_KEY_PREFIX = "sportsbooking:slot:";
    private static final String HOLD_KEY_PREFIX = "sportsbooking:hold:";
    private static final String USER_VENUE_LOCK_PREFIX = "sportsbooking:user_active_venue:";

    // Payment method ID for manual host booking
    private static final Integer PAYMENT_METHOD_CASH = 500;

    @Override
    public SlotStatusResponse getAvailableSlots(UUID userId, UUID venueId, LocalDate date) {
        int dayOfWeek = date.getDayOfWeek().getValue(); // 1=MON, 7=SUN
        List<AvailableVenueEntity> schedules = availableVenueRepository.findByVenueIdAndStatus(venueId,
                VenueStatus.ACTIVE.getValue());

        int actualBuffer = slotBufferMinutes;
        if (userId != null) {
            java.util.Optional<VenueEntity> venueOpt = venueRepository.findById(venueId);
            if (venueOpt.isPresent() && venueOpt.get().getHostUserId().equals(userId)) {
                // Bypass the time buffer for the host
                actualBuffer = 0;
            }
        }

        List<SlotAvailability> daySlots = new ArrayList<>();
        List<SlotAvailability> nightSlots = new ArrayList<>();

        // Get booked slots for this date
        List<BookingEntity> activeBookings = bookingRepository.findByVenueIdAndBookingDateAndStatusInWithDetails(
                venueId, date, List.of(BookingStatus.PENDING.getValue(), BookingStatus.CONFIRMED.getValue()));
        Set<String> bookedSlotKeys = new HashSet<>();
        for (BookingEntity b : activeBookings) {
            for (BookingDetailEntity detail : b.getDetails()) {
                bookedSlotKeys.add(generateSlotIdentifier(detail.getScheduleType(), detail.getStartTime()));
            }
        }

        for (AvailableVenueEntity schedule : schedules) {
            if (!Arrays.asList(schedule.getDays()).contains(dayOfWeek)) {
                continue;
            }

            LocalTime current = schedule.getStartTime();
            LocalTime end = schedule.getEndTime();

            java.time.LocalDate todayInLima = DateUtilities.currentLocalDate();
            java.time.LocalTime nowInLima = DateUtilities.currentLocalTime();
            boolean isToday = date.equals(todayInLima);

            while (current.isBefore(end) || current.equals(end)) {
                LocalTime next = current.plusHours(1);

                if (isToday && current.isBefore(nowInLima.plusMinutes(actualBuffer))) {
                    current = next;
                    continue;
                }
                String slotId = generateSlotIdentifier(schedule.getScheduleType(), current);
                String redisKey = generateSlotRedisKey(venueId, date, schedule.getScheduleType(), current);

                String status = "AVAILABLE";
                if (bookedSlotKeys.contains(slotId)) {
                    status = "BOOKED";
                } else if (safeRedisHasKey(redisKey)) {
                    status = "HELD";
                }

                SlotAvailability slotAv = SlotAvailability.builder()
                        .id(slotId)
                        .startTime(current)
                        .endTime(next)
                        .price(schedule.getPrice())
                        .status(status)
                        .build();

                if (schedule.getScheduleType() == ScheduleType.DAY.getValue()) {
                    daySlots.add(slotAv);
                } else if (schedule.getScheduleType() == ScheduleType.NIGHT.getValue()) {
                    nightSlots.add(slotAv);
                }
                current = next;
                if (current.equals(LocalTime.MIDNIGHT) || current.isBefore(schedule.getStartTime())) {
                    break;
                }
            }
        }

        return SlotStatusResponse.builder()
                .daySlots(daySlots)
                .nightSlots(nightSlots)
                .build();
    }

    @Override
    public HoldResponse holdSlots(UUID userId, HoldSlotsRequest request) {
        validateConsecutiveSlots(request.getSlots());

        String holdId = UUID.randomUUID().toString();
        String holdKey = HOLD_KEY_PREFIX + holdId;
        String userLockKey = USER_VENUE_LOCK_PREFIX + userId.toString();

        String activeVenue = redisTemplate.opsForValue().get(userLockKey);
        if (activeVenue != null && !activeVenue.equals(request.getVenueId().toString())) {
            throw new BusinessException(
                    "Ya tienes horarios en proceso de reserva en otra cancha. Termina o cancela esa reservacion primero.");
        }

        long actualBackendTtlSeconds = (baseHoldTtlMinutes * 60L) + holdTtlBufferSeconds;

        // Try to acquire locks
        List<String> acquiredKeys = new ArrayList<>();
        try {
            for (SlotItem slot : request.getSlots()) {
                String redisKey = generateSlotRedisKey(request.getVenueId(), request.getDate(), slot.getScheduleType(),
                        slot.getStartTime());
                Boolean acquired = redisTemplate.opsForValue().setIfAbsent(redisKey, userId.toString(),
                        actualBackendTtlSeconds, TimeUnit.SECONDS);
                if (Boolean.TRUE.equals(acquired)) {
                    acquiredKeys.add(redisKey);
                } else {
                    throw new BusinessException("Uno o más horarios seleccionados ya no están disponibles");
                }
            }

            // Save hold metadata
            HoldMetadata metadata = new HoldMetadata(userId, request.getVenueId(), request.getDate(),
                    request.getSlots());
            redisTemplate.opsForValue().set(holdKey, objectMapper.writeValueAsString(metadata), actualBackendTtlSeconds,
                    TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(userLockKey, request.getVenueId().toString(), actualBackendTtlSeconds,
                    TimeUnit.SECONDS);

            OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(actualBackendTtlSeconds);
            return new HoldResponse(holdId, expiresAt, baseHoldTtlMinutes);

        } catch (Exception e) {
            // Rollback acquired locks
            for (String key : acquiredKeys) {
                redisTemplate.delete(key);
            }
            if (e instanceof BusinessException) {
                throw (BusinessException) e;
            }
            throw new RuntimeException("Error bloqueando horarios", e);
        }
    }

    @Override
    public SlotValidationResponse validateSlotAvailability(HoldSlotsRequest request) {
        List<SlotItemResult> available = new ArrayList<>();
        List<SlotItemResult> unavailable = new ArrayList<>();

        // Get booked slots for this date
        List<BookingEntity> activeBookings = bookingRepository.findByVenueIdAndBookingDateAndStatusInWithDetails(
                request.getVenueId(), request.getDate(),
                List.of(BookingStatus.PENDING.getValue(), BookingStatus.CONFIRMED.getValue()));
        Set<String> bookedSlotKeys = new HashSet<>();
        for (BookingEntity b : activeBookings) {
            for (BookingDetailEntity detail : b.getDetails()) {
                bookedSlotKeys.add(generateSlotIdentifier(detail.getScheduleType(), detail.getStartTime()));
            }
        }

        for (SlotItem slot : request.getSlots()) {
            String slotId = generateSlotIdentifier(slot.getScheduleType(), slot.getStartTime());
            String redisKey = generateSlotRedisKey(request.getVenueId(), request.getDate(), slot.getScheduleType(),
                    slot.getStartTime());

            SlotItemResult result = new SlotItemResult(
                    slot.getScheduleType(),
                    slot.getStartTime().toString(),
                    null);

            if (bookedSlotKeys.contains(slotId)) {
                unavailable.add(
                        new SlotItemResult(slot.getScheduleType(), slot.getStartTime().toString(), "ALREADY_BOOKED"));
            } else if (safeRedisHasKey(redisKey)) {
                unavailable.add(
                        new SlotItemResult(slot.getScheduleType(), slot.getStartTime().toString(), "HELD_BY_OTHER"));
            } else {
                available.add(result);
            }
        }

        return new SlotValidationResponse(available, unavailable);
    }

    @Override
    @Transactional
    public BookingResponse confirmBooking(UUID userId, String holdId, MultipartFile voucher, Integer idPaymentMethod,
            String description, List<Integer> sportsId) {
        String holdKey = HOLD_KEY_PREFIX + holdId;
        String metaStr = redisTemplate.opsForValue().get(holdKey);
        if (metaStr == null) {
            throw new BusinessException("El bloqueo temporal ha expirado o no existe");
        }

        HoldMetadata metadata;
        try {
            metadata = objectMapper.readValue(metaStr, HoldMetadata.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error leyendo datos del bloqueo temporal", e);
        }

        if (!metadata.getUserId().equals(userId)) {
            throw new BusinessException("No eres propietario de este bloqueo temporal");
        }

        VenueEntity venue = venueRepository.findById(metadata.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue", metadata.getVenueId()));

        // Ensure slots are still locked
        List<String> slotKeys = new ArrayList<>();
        for (SlotItem slot : metadata.getSlots()) {
            String redisKey = generateSlotRedisKey(metadata.getVenueId(), metadata.getDate(), slot.getScheduleType(),
                    slot.getStartTime());
            slotKeys.add(redisKey);
            String owner = redisTemplate.opsForValue().get(redisKey);
            if (!userId.toString().equals(owner)) {
                throw new BusinessException("El bloqueo expiró en uno o más horarios");
            }
        }

        boolean isSelfHost = venue.getHostUserId().equals(userId);

        if (!isSelfHost && idPaymentMethod != null && idPaymentMethod.equals(PAYMENT_METHOD_CASH)) {
            throw new BusinessException("Método de pago no disponible para clientes regulares");
        }

        if (!isSelfHost && (voucher == null || voucher.isEmpty())) {
            throw new BusinessException("El comprobante de pago es requerido");
        }

        // Upload voucher
        String voucherUrl = null;
        if (voucher != null && !voucher.isEmpty()) {
            voucherUrl = fileStorageService.store(voucher, venue.getId());
        }

        // Fetch valid prices from available_venues
        List<AvailableVenueEntity> availableVenues = availableVenueRepository.findByVenueIdAndStatus(venue.getId(),
                VenueStatus.ACTIVE.getValue());
        BigDecimal basePriceDay = null;
        BigDecimal basePriceNight = null;
        for (AvailableVenueEntity av : availableVenues) {
            if (av.getScheduleType() == ScheduleType.DAY.getValue())
                basePriceDay = av.getPrice();
            if (av.getScheduleType() == ScheduleType.NIGHT.getValue())
                basePriceNight = av.getPrice();
        }

        // Fallback: si no hay precios configurados en available_venues, usar el
        // basePrice del venue
        if (basePriceDay == null)
            basePriceDay = venue.getBasePrice();
        if (basePriceNight == null)
            basePriceNight = venue.getBasePrice();

        // Calculate totals and create details
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<BookingDetailEntity> details = new ArrayList<>();

        for (SlotItem slot : metadata.getSlots()) {
            BigDecimal price = (slot.getScheduleType() == ScheduleType.DAY.getValue()) ? basePriceDay : basePriceNight;
            if (price == null) {
                throw new BusinessException("Precio no configurado para el tipo de horario " + slot.getScheduleType());
            }

            BookingDetailEntity detail = BookingDetailEntity.builder()
                    .scheduleType(slot.getScheduleType())
                    .startTime(slot.getStartTime())
                    .endTime(slot.getStartTime().plusHours(1))
                    .price(price)
                    .build();
            details.add(detail);
            totalAmount = totalAmount.add(price);
        }

        Integer[] finalSportsId = (sportsId != null && !sportsId.isEmpty())
                ? sportsId.toArray(new Integer[0])
                : (venue.getIdSports() != null ? venue.getIdSports() : new Integer[0]);

        log.info("[confirmBooking] Received sportsId: {}, finalSportsId assigned to entity: {}", sportsId, java.util.Arrays.toString(finalSportsId));

        Double paramValue = parameterRepository.findById(57)
                .map(ParameterEntity::getValue)
                .orElse(10.0);

        BigDecimal commissionRateVal = BigDecimal.valueOf(paramValue);
        BigDecimal rateMultiplier = paramValue >= 1.0
                ? BigDecimal.valueOf(paramValue / 100.0)
                : BigDecimal.valueOf(paramValue);

        BigDecimal commissionAmountVal = totalAmount.multiply(rateMultiplier)
                .setScale(2, java.math.RoundingMode.HALF_UP);

        BigDecimal netAmountVal = totalAmount.subtract(commissionAmountVal)
                .setScale(2, java.math.RoundingMode.HALF_UP);

        BookingEntity booking = BookingEntity.builder()
                .venueId(venue.getId())
                .clientUserId(userId)
                .bookingDate(metadata.getDate())
                .totalHours(metadata.getSlots().size())
                .totalAmount(totalAmount)
                .commissionRate(commissionRateVal)
                .commissionAmount(commissionAmountVal)
                .netAmount(netAmountVal)
                .status(isSelfHost ? BookingStatus.CONFIRMED.getValue() : BookingStatus.PENDING.getValue())
                .isSelfHost(isSelfHost)
                .sportsId(finalSportsId)
                .basePriceDay(basePriceDay)
                .basePriceNight(basePriceNight)
                .idPaymentMethod(idPaymentMethod)
                .voucherUrl(voucherUrl)
                .description(description)
                .build();

        if (isSelfHost) {
            booking.setConfirmedAt(OffsetDateTime.now(ZoneOffset.UTC));
        }

        for (BookingDetailEntity d : details) {
            booking.addDetail(d);
        }

        String generatedCode = bookingCodeGenerator.generateUniqueCode();

        booking.setBookingCode(generatedCode);

        BookingEntity savedBooking = bookingRepository.saveAndFlush(booking);

        // La deuda en host_debts se registrará únicamente cuando la reserva sea confirmada
        // (estado CONFIRMED = 3), ya sea por el anfitrión (confirmByHost) o por el sistema (confirmBySystem).

        // Cleanup Redis
        redisTemplate.delete(holdKey);
        redisTemplate.delete(slotKeys);
        redisTemplate.delete(USER_VENUE_LOCK_PREFIX + userId.toString());

        // Obtenemos al cliente para personalizar el mensaje
        UserEntity client = userRepository.findById(userId).orElse(null);
        String clientName = client != null ? client.getName() : "un usuario";

        // SEND NOTIFICATION TO HOST
        notificationService.sendPushAndSave(
                venue.getHostUserId(),
                "Nueva Solicitud de Reserva",
                "Tienes una nueva solicitud de reserva de **" + clientName + "** pendiente por confirmar.",
                NotificationType.NEW_BOOKING,
                savedBooking.getId(),
                java.util.Map.of("route", RouteConstants.HOST_RESERVATIONS_STANDALONE));

        List<Integer> bSports = (savedBooking.getSportsId() != null && savedBooking.getSportsId().length > 0)
                ? java.util.Arrays.asList(savedBooking.getSportsId())
                : (venue.getIdSports() != null ? java.util.Arrays.asList(venue.getIdSports()) : java.util.List.of());
        String resolvedSportName = resolveSportNameFromMap(bSports, getSportsParameterMap());

        return BookingResponse.builder()
                .id(savedBooking.getId())
                .venueId(savedBooking.getVenueId())
                .clientUserId(userId)
                .bookingDate(savedBooking.getBookingDate())
                .totalHours(savedBooking.getTotalHours())
                .totalAmount(savedBooking.getTotalAmount())
                .commissionRate(savedBooking.getCommissionRate())
                .commissionAmount(savedBooking.getCommissionAmount())
                .netAmount(savedBooking.getNetAmount())
                .status(savedBooking.getStatus())
                .idPaymentMethod(savedBooking.getIdPaymentMethod())
                .bookingCode(savedBooking.getBookingCode())
                .sportsId(bSports)
                .sportName(resolvedSportName)
                .build();
    }

    @Override
    @Transactional
    public BookingResponse uploadVoucher(UUID userId, UUID bookingId, MultipartFile voucher) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if (!booking.getClientUserId().equals(userId)) {
            throw new BusinessException("No eres propietario de esta reserva");
        }

        if (voucher == null || voucher.isEmpty()) {
            throw new BusinessException("El comprobante de pago es requerido");
        }

        VenueEntity venue = venueRepository.findById(booking.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue", booking.getVenueId()));

        String filename = fileStorageService.store(voucher, venue.getId());
        booking.setVoucherUrl(filename);
        bookingRepository.save(booking);

        // Notify host that a voucher was uploaded
        UserEntity client = userRepository.findById(userId).orElse(null);
        String clientName = client != null ? client.getName() : "un usuario";

        notificationService.sendPushAndSave(
                venue.getHostUserId(),
                "Nuevo comprobante subido",
                "El cliente **" + clientName + "** ha subido un comprobante para la reserva.",
                NotificationType.NEW_BOOKING,
                booking.getId(),
                Map.of("route", RouteConstants.HOST_RESERVATIONS_STANDALONE));

        List<Integer> bSports = (booking.getSportsId() != null && booking.getSportsId().length > 0)
                ? java.util.Arrays.asList(booking.getSportsId())
                : (venue.getIdSports() != null ? java.util.Arrays.asList(venue.getIdSports()) : java.util.List.of());
        String resolvedSportName = resolveSportNameFromMap(bSports, getSportsParameterMap());

        return BookingResponse.builder()
                .id(booking.getId())
                .venueId(booking.getVenueId())
                .clientUserId(booking.getClientUserId())
                .bookingDate(booking.getBookingDate())
                .totalHours(booking.getTotalHours())
                .totalAmount(booking.getTotalAmount())
                .commissionRate(booking.getCommissionRate())
                .commissionAmount(booking.getCommissionAmount())
                .netAmount(booking.getNetAmount())
                .status(booking.getStatus())
                .idPaymentMethod(booking.getIdPaymentMethod())
                .bookingCode(booking.getBookingCode())
                .sportsId(bSports)
                .sportName(resolvedSportName)
                .build();
    }

    @Override
    public BookingResponse getBookingById(UUID bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        List<Integer> bSports = (booking.getSportsId() != null && booking.getSportsId().length > 0)
                ? java.util.Arrays.asList(booking.getSportsId())
                : java.util.List.of();
        String resolvedSportName = resolveSportNameFromMap(bSports, getSportsParameterMap());

        return BookingResponse.builder()
                .id(booking.getId())
                .venueId(booking.getVenueId())
                .clientUserId(booking.getClientUserId())
                .bookingDate(booking.getBookingDate())
                .totalHours(booking.getTotalHours())
                .totalAmount(booking.getTotalAmount())
                .commissionRate(booking.getCommissionRate())
                .commissionAmount(booking.getCommissionAmount())
                .netAmount(booking.getNetAmount())
                .status(booking.getStatus())
                .idPaymentMethod(booking.getIdPaymentMethod())
                .bookingCode(booking.getBookingCode())
                .sportsId(bSports)
                .sportName(resolvedSportName)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> getVoucherResource(UUID userId, UUID bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if (booking.getVoucherUrl() == null || booking.getVoucherUrl().isEmpty()) {
            throw new BusinessException("No existe comprobante asociado a esta reserva");
        }

        VenueEntity venue = venueRepository.findById(booking.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue", booking.getVenueId()));

        // Authorization: el cliente que hizo la reserva o el host del venue pueden ver
        // el voucher
        if (!booking.getClientUserId().equals(userId) && !venue.getHostUserId().equals(userId)) {
            throw new AccessDeniedException("No tienes permisos para ver este comprobante");
        }

        Resource resource = fileStorageService.load(venue.getId(), booking.getVoucherUrl());

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + booking.getVoucherUrl() + "\"")
                .body(resource);
    }

    @Override
    public void releaseHold(UUID userId, String holdId) {
        String holdKey = HOLD_KEY_PREFIX + holdId;
        String metaStr = redisTemplate.opsForValue().get(holdKey);
        if (metaStr != null) {
            try {
                HoldMetadata metadata = objectMapper.readValue(metaStr, HoldMetadata.class);
                if (metadata.getUserId().equals(userId)) {
                    List<String> slotKeys = metadata.getSlots().stream()
                            .map(slot -> generateSlotRedisKey(metadata.getVenueId(), metadata.getDate(),
                                    slot.getScheduleType(), slot.getStartTime()))
                            .collect(Collectors.toList());
                    redisTemplate.delete(slotKeys);
                    redisTemplate.delete(holdKey);
                    redisTemplate.delete(USER_VENUE_LOCK_PREFIX + userId.toString());
                }
            } catch (JsonProcessingException e) {
                log.error("Error al parsear metadata del bloqueo temporal durante la liberación", e);
            }
        }
    }

    @Override
    @Transactional
    public void confirmByHost(UUID hostId, UUID bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        VenueEntity venue = venueRepository.findById(booking.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue", booking.getVenueId()));

        if (!venue.getHostUserId().equals(hostId)) {
            throw new BusinessException("No eres el anfitrión de esta cancha principal.");
        }

        booking.setStatus(BookingStatus.CONFIRMED.getValue());
        booking.setConfirmedAt(OffsetDateTime.now(ZoneOffset.UTC));
        bookingRepository.saveAndFlush(booking);

        try {
            commissionService.registerHostDebtForBooking(booking);
        } catch (Exception e) {
            log.warn("No se pudo registrar la deuda en host_debts al confirmar la reserva: {}", e.getMessage(), e);
        }

        notificationService.sendPushAndSave(
                booking.getClientUserId(),
                "Reserva Confirmada",
                "Tu reserva en **" + venue.getName() + "** ha sido confirmada.",
                NotificationType.BOOKING_CONFIRMED,
                booking.getId(),
                Map.of("route", RouteConstants.RESERVATIONS_STANDALONE));
    }

    @Override
    @Transactional
    public void confirmBySystem(UUID bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        VenueEntity venue = venueRepository.findById(booking.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue", booking.getVenueId()));

        booking.setStatus(BookingStatus.CONFIRMED.getValue());
        booking.setConfirmedAt(OffsetDateTime.now(ZoneOffset.UTC));
        bookingRepository.saveAndFlush(booking);

        try {
            commissionService.registerHostDebtForBooking(booking);
        } catch (Exception e) {
            log.warn("No se pudo registrar la deuda en host_debts al confirmar por sistema la reserva: {}", e.getMessage(), e);
        }

        notificationService.sendPushAndSave(
                booking.getClientUserId(),
                "Reserva Confirmada",
                "Tu reserva en **" + venue.getName() + "** ha sido confirmada.",
                NotificationType.BOOKING_CONFIRMED,
                booking.getId(),
                Map.of("route", RouteConstants.RESERVATIONS_STANDALONE));
    }

    @Override
    @Transactional
    public void rejectByHost(UUID hostId, UUID bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        VenueEntity venue = venueRepository.findById(booking.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue", booking.getVenueId()));

        if (!venue.getHostUserId().equals(hostId)) {
            throw new BusinessException("No eres el anfitrión de esta cancha principal.");
        }

        booking.setStatus(BookingStatus.REJECTED.getValue());
        booking.setRejectedAt(OffsetDateTime.now(ZoneOffset.UTC));
        bookingRepository.save(booking);

        try {
            commissionService.calculateCurrentCycleCommission(venue.getId());
        } catch (Exception e) {
            log.warn("No se pudo actualizar host_debts al rechazar la reserva: {}", e.getMessage());
        }

        notificationService.sendPushAndSave(
                booking.getClientUserId(),
                "Reserva Rechazada",
                "Tu reserva en **" + venue.getName() + "** ha sido rechazada por el anfitrión.",
                NotificationType.BOOKING_REJECTED,
                booking.getId(),
                Map.of("route", RouteConstants.RESERVATIONS_STANDALONE));
    }

    private boolean isBookingExpired(LocalDate bookingDate, List<BookingDetailEntity> details, LocalDate currentDate,
            LocalTime currentTime) {
        if (bookingDate.isBefore(currentDate)) {
            return true;
        } else if (bookingDate.isEqual(currentDate) && details != null && !details.isEmpty()) {
            BookingDetailEntity last = details.get(details.size() - 1);
            if (last.getEndTime().isBefore(currentTime)) {
                return true;
            }
        }
        return false;
    }

    private String getBookingTimeSlot(List<BookingDetailEntity> details) {
        if (details == null || details.isEmpty())
            return "";
        List<BookingDetailEntity> sorted = new java.util.ArrayList<>(details);
        sorted.sort(java.util.Comparator.comparing(BookingDetailEntity::getStartTime));

        java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
        java.util.List<String> ranges = new java.util.ArrayList<>();

        LocalTime currentStart = sorted.get(0).getStartTime();
        LocalTime currentEnd = sorted.get(0).getEndTime();

        for (int i = 1; i < sorted.size(); i++) {
            BookingDetailEntity current = sorted.get(i);
            if (current.getStartTime().equals(currentEnd)) {
                // Son consecutivos
                currentEnd = current.getEndTime();
            } else {
                // Se rompió la secuencia
                ranges.add(currentStart.format(timeFormatter) + " - " + currentEnd.format(timeFormatter));
                currentStart = current.getStartTime();
                currentEnd = current.getEndTime();
            }
        }
        // Agregamos el último rango formado
        ranges.add(currentStart.format(timeFormatter) + " - " + currentEnd.format(timeFormatter));

        return String.join(", ", ranges);
    }

    private void validateConsecutiveSlots(List<SlotItem> slots) {
        if (slots == null || slots.isEmpty())
            return;

        Map<Integer, List<LocalTime>> slotsByType = slots.stream()
                .collect(Collectors.groupingBy(SlotItem::getScheduleType,
                        Collectors.mapping(SlotItem::getStartTime, Collectors.toList())));

        for (List<LocalTime> times : slotsByType.values()) {
            if (times.size() <= 1)
                continue;

            Collections.sort(times);
            for (int i = 0; i < times.size() - 1; i++) {
                if (!times.get(i).plusHours(1).equals(times.get(i + 1))) {
                    throw new BusinessException("Los horarios seleccionados deben ser consecutivos");
                }
            }
        }
    }

    private String generateSlotIdentifier(int scheduleType, LocalTime startTime) {
        return scheduleType + "-" + startTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private String generateSlotRedisKey(UUID venueId, LocalDate date, int scheduleType, LocalTime startTime) {
        return SLOT_KEY_PREFIX + venueId + ":" + date.format(DateTimeFormatter.ISO_LOCAL_DATE) + ":" + scheduleType
                + ":" + startTime.format(DateTimeFormatter.ofPattern("HH"));
    }

    /**
     * Wrapper seguro para `redisTemplate.hasKey` que devuelve `false` si Redis no
     * está disponible.
     */
    private boolean safeRedisHasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("Redis no disponible al comprobar clave {}: {}. Asumiendo que la clave no existe.", key,
                    e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingByHost(UUID hostId) {
        return bookingRepository.countPendingByHostUserId(hostId, BookingStatus.PENDING.getValue());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HostBookingResponse> getHostBookings(UUID hostId) {
        List<BookingEntity> bookings = bookingRepository.findBookingsByHostUserId(hostId);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        Map<UUID, HostBookingResponse> distinctBookings = new java.util.LinkedHashMap<>();

        LocalDate currentDate = DateUtilities.currentLocalDate();
        LocalTime currentTime = DateUtilities.currentLocalTime();

        Map<Integer, String> sportsMap = getSportsParameterMap();

        for (BookingEntity b : bookings) {
            if (distinctBookings.containsKey(b.getId()))
                continue;
            VenueEntity venue = venueRepository.findById(b.getVenueId()).orElse(null);
            UserEntity client = userRepository.findById(b.getClientUserId()).orElse(null);

            String venueName = venue != null ? venue.getName() : "Cancha Desconocida";
            String clientName = client != null ? client.getName() : "Desconocido";
            String clientPhone = client != null ? client.getPhone() : "";

            String timeSlot = getBookingTimeSlot(b.getDetails());
            boolean isExpired = isBookingExpired(b.getBookingDate(), b.getDetails(), currentDate, currentTime);

            BigDecimal commRate = b.getCommissionRate();
            BigDecimal commAmount = b.getCommissionAmount();
            BigDecimal netAmt = b.getNetAmount();

            if (commRate == null || commAmount == null || netAmt == null) {
                BigDecimal rate = commRate != null ? commRate : BigDecimal.valueOf(10.0);
                BigDecimal multiplier = rate.compareTo(BigDecimal.ONE) >= 0
                        ? rate.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP)
                        : rate;
                BigDecimal total = b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO;
                if (commAmount == null) {
                    commAmount = total.multiply(multiplier).setScale(2, java.math.RoundingMode.HALF_UP);
                }
                if (netAmt == null) {
                    netAmt = total.subtract(commAmount).setScale(2, java.math.RoundingMode.HALF_UP);
                }
                if (commRate == null) {
                    commRate = rate;
                }
            }

            boolean isPaid = (b.getVoucherUrl() != null && !b.getVoucherUrl().isEmpty())
                    || Boolean.TRUE.equals(b.getIsSelfHost())
                    || b.getStatus() == BookingStatus.CONFIRMED.getValue()
                    || b.getStatus() == BookingStatus.COMPLETED.getValue();

            List<Integer> bSports = (b.getSportsId() != null && b.getSportsId().length > 0)
                    ? java.util.Arrays.asList(b.getSportsId())
                    : (venue != null && venue.getIdSports() != null ? java.util.Arrays.asList(venue.getIdSports()) : java.util.List.of());

            List<String> eventTypes = new ArrayList<>();
            for (Integer sId : bSports) {
                if (sId == null) continue;
                String name = sportsMap.get(sId);
                if (name == null) {
                    try {
                        name = parameterRepository.findById(sId)
                                .map(ParameterEntity::getChildName)
                                .orElse(null);
                        if (name != null) {
                            sportsMap.put(sId, name);
                        }
                    } catch (Exception ignored) {}
                }
                if (name != null && !eventTypes.contains(name)) {
                    eventTypes.add(name);
                }
            }
            String eventType = eventTypes.isEmpty() ? null : String.join(", ", eventTypes);

            HostBookingResponse response = HostBookingResponse.builder()
                    .id(b.getId())
                    .venueName(venueName)
                    .code(b.getBookingCode())
                    .date(b.getBookingDate())
                    .timeSlot(timeSlot)
                    .clientName(clientName)
                    .clientPhone(clientPhone)
                    .amount(b.getTotalAmount())
                    .commissionRate(commRate)
                    .commissionAmount(commAmount)
                    .netAmount(netAmt)
                    .status(b.getStatus())
                    .paid(isPaid)
                    .isExpired(isExpired)
                    .voucherUrl(b.getVoucherUrl())
                    .message(b.getDescription())
                    .confirmedAt(b.getConfirmedAt())
                    .eventType(eventType)
                    .eventTypes(eventTypes)
                    .sportName(eventType)
                    .build();
            distinctBookings.put(b.getId(), response);
        }
        return new java.util.ArrayList<>(distinctBookings.values());
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.sportsbooking.mainapi.dto.booking.response.HostBookingSummaryResponse> getHostBookingSummaries(
            UUID hostId) {
        List<BookingEntity> bookings = bookingRepository.findBookingsByHostUserId(hostId);

        UserEntity host = userRepository.findById(hostId).orElse(null);
        String hostName = host != null ? host.getName() : "Anfitrión Desconocido";

        // Load sports parameters (parentId=1 : deportes)
        Map<Integer, String> sportsMap = new HashMap<>();
        try {
            List<com.sportsbooking.mainapi.dto.parameter.ParameterDto> params = parameterService.getByParent(1);
            for (com.sportsbooking.mainapi.dto.parameter.ParameterDto p : params) {
                if (p.getIdParam() != null) {
                    sportsMap.put(p.getIdParam(), p.getChildName());
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo cargar parámetros de deportes: {}", e.getMessage());
        }

        List<com.sportsbooking.mainapi.dto.booking.response.HostBookingSummaryResponse> out = new ArrayList<>();

        for (BookingEntity b : bookings) {
            VenueEntity venue = venueRepository.findById(b.getVenueId()).orElse(null);
            String venueName = venue != null ? venue.getName() : "Cancha Desconocida";

            // Map sports ids to names
            List<String> eventTypes = new ArrayList<>();
            Integer[] sports = b.getSportsId();
            if (sports != null) {
                for (Integer s : sports) {
                    if (s == null)
                        continue;
                    String name = sportsMap.getOrDefault(s, "Desconocido");
                    if (!eventTypes.contains(name))
                        eventTypes.add(name);
                }
            }

            // Rating summary for venue
            double avg = 0.0;
            long count = 0;
            if (venue != null) {
                try {
                    com.sportsbooking.mainapi.dto.review.response.VenueRatingSummary rs = reviewService
                            .getRatingSummary(venue.getId());
                    if (rs != null) {
                        avg = rs.avgRating();
                        count = rs.reviewCount();
                    }
                } catch (Exception e) {
                    log.warn("No se pudo obtener rating para venue {}: {}", venue.getId(), e.getMessage());
                }
            }

            // Schedules as list of ranges
            String slotStr = getBookingTimeSlot(b.getDetails());
            List<String> schedules = slotStr == null || slotStr.isBlank() ? List.of()
                    : Arrays.stream(slotStr.split("\\s*,\\s*")).toList();

            out.add(new com.sportsbooking.mainapi.dto.booking.response.HostBookingSummaryResponse(
                    b.getId(),
                    b.getVenueId(),
                    venueName,
                    hostId,
                    hostName,
                    b.getBookingDate(),
                    b.getCreatedAt(),
                    b.getTotalAmount(),
                    eventTypes,
                    avg,
                    count,
                    schedules,
                    b.getStatus(),
                    b.getVoucherUrl()));
        }

        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.sportsbooking.mainapi.dto.booking.response.HostBookingSummaryResponse> getAllHostBookingSummaries() {
        List<BookingEntity> bookings = bookingRepository.findAll();

        // Carga eficiente de entidades relacionadas para evitar N+1 queries
        List<UUID> venueIds = bookings.stream().map(BookingEntity::getVenueId).distinct().collect(Collectors.toList());
        Map<UUID, VenueEntity> venueMap = venueRepository.findAllById(venueIds).stream()
                .collect(Collectors.toMap(VenueEntity::getId, venue -> venue));

        List<UUID> hostIds = venueMap.values().stream().map(VenueEntity::getHostUserId).distinct()
                .collect(Collectors.toList());
        Map<UUID, UserEntity> hostMap = userRepository.findAllById(hostIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, user -> user));

        // Cargar parámetros de deportes (cacheado)
        Map<Integer, String> sportsMap = new HashMap<>();
        try {
            List<com.sportsbooking.mainapi.dto.parameter.ParameterDto> params = parameterService.getByParent(1);
            for (com.sportsbooking.mainapi.dto.parameter.ParameterDto p : params) {
                if (p.getIdParam() != null) {
                    sportsMap.put(p.getIdParam(), p.getChildName());
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo cargar parámetros de deportes: {}", e.getMessage());
        }

        List<com.sportsbooking.mainapi.dto.booking.response.HostBookingSummaryResponse> out = new ArrayList<>();

        for (BookingEntity b : bookings) {
            VenueEntity venue = venueMap.get(b.getVenueId());
            if (venue == null)
                continue;

            UserEntity host = hostMap.get(venue.getHostUserId());
            UUID hostId = (host != null) ? host.getId() : null;
            String hostName = (host != null) ? host.getName() : "Anfitrión Desconocido";

            String venueName = venue.getName();

            List<String> eventTypes = new ArrayList<>();
            Integer[] sports = b.getSportsId();
            if (sports != null) {
                for (Integer s : sports) {
                    if (s == null)
                        continue;
                    String name = sportsMap.getOrDefault(s, "Desconocido");
                    if (!eventTypes.contains(name))
                        eventTypes.add(name);
                }
            }

            com.sportsbooking.mainapi.dto.review.response.VenueRatingSummary rs = reviewService
                    .getRatingSummary(venue.getId());

            String slotStr = getBookingTimeSlot(b.getDetails());
            List<String> schedules = slotStr == null || slotStr.isBlank() ? List.of()
                    : Arrays.stream(slotStr.split("\\s*,\\s*")).toList();

            // NOTA: Esto asume que HostBookingSummaryResponse ha sido actualizado para
            // incluir los detalles del anfitrión.
            out.add(new com.sportsbooking.mainapi.dto.booking.response.HostBookingSummaryResponse(
                    b.getId(), b.getVenueId(), venueName, hostId, hostName, b.getBookingDate(), b.getCreatedAt(),
                    b.getTotalAmount(), eventTypes, rs.avgRating(), rs.reviewCount(), schedules, b.getStatus(),
                    b.getVoucherUrl()));
        }

        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientBookingResponse> getClientBookings(UUID clientId) {
        List<Object[]> results = bookingRepository.findBookingsWithVenueByClientUserId(clientId);

        Map<UUID, ClientBookingResponse> distinctBookings = new java.util.LinkedHashMap<>();

        LocalDate currentDate = DateUtilities.currentLocalDate();
        LocalTime currentTime = DateUtilities.currentLocalTime();

        Map<Integer, String> sportsMap = getSportsParameterMap();

        for (Object[] row : results) {
            BookingEntity booking = (BookingEntity) row[0];
            VenueEntity venue = (VenueEntity) row[1];

            if (distinctBookings.containsKey(booking.getId()))
                continue;

            String timeSlot = getBookingTimeSlot(booking.getDetails());
            boolean isExpired = isBookingExpired(booking.getBookingDate(), booking.getDetails(), currentDate,
                    currentTime);

            String code = booking.getBookingCode();

            String imageUrl = "";
            List<VenuePhotoEntity> photos = venuePhotoRepository.findByVenueId(venue.getId());
            if (!photos.isEmpty()) {
                imageUrl = photos.get(0).getId().toString();
            }

            // Fetch user review if exists
            Float points = null;
            String comment = null;
            if (booking.getStatus() == BookingStatus.COMPLETED.getValue()) {
                Optional<VenueReviewEntity> reviewOpt = venueReviewRepository.findByBookingIdAndUserId(booking.getId(),
                        clientId);
                if (reviewOpt.isPresent()) {
                    points = reviewOpt.get().getPoints().floatValue();
                    comment = reviewOpt.get().getDescription();
                }
            }

            List<Integer> bSports = (booking.getSportsId() != null && booking.getSportsId().length > 0)
                    ? java.util.Arrays.asList(booking.getSportsId())
                    : (venue.getIdSports() != null ? java.util.Arrays.asList(venue.getIdSports()) : java.util.List.of());

            String resolvedSportName = resolveSportNameFromMap(bSports, sportsMap);

            ClientBookingResponse response = ClientBookingResponse.builder()
                    .id(booking.getId())
                    .venueName(venue.getName())
                    .venueImageUrl(imageUrl)
                    .code(code)
                    .date(booking.getBookingDate())
                    .timeSlot(timeSlot)
                    .venueId(venue.getId())
                    .latitude(venue.getLocation() != null ? venue.getLocation().getY() : 0.0)
                    .longitude(venue.getLocation() != null ? venue.getLocation().getX() : 0.0)
                    .address(venue.getAddress())
                    .ubigeo(ubigeoService.getUbigeoText(venue.getUbigeo()))
                    .contactNumber(venue.getContactNumber())
                    .venueSportIds(bSports)
                    .price(booking.getTotalAmount())
                    .status(booking.getStatus())
                    .sportName(resolvedSportName)
                    .isExpired(isExpired)
                    .userReviewPoints(points)
                    .userReviewComment(comment)
                    .build();
            distinctBookings.put(booking.getId(), response);
        }
        return new java.util.ArrayList<>(distinctBookings.values());
    }

    private Map<Integer, String> getSportsParameterMap() {
        Map<Integer, String> sportsMap = new HashMap<>();
        try {
            List<com.sportsbooking.mainapi.dto.parameter.ParameterDto> params = parameterService.getByParent(1);
            if (params != null) {
                for (com.sportsbooking.mainapi.dto.parameter.ParameterDto p : params) {
                    sportsMap.put(p.getIdParam(), p.getChildName());
                }
            }
        } catch (Exception e) {
            log.warn("Error cargando mapa de deportes: {}", e.getMessage());
        }
        return sportsMap;
    }

    private String resolveSportNameFromMap(List<Integer> sportsIds, Map<Integer, String> sportsMap) {
        if (sportsIds == null || sportsIds.isEmpty()) {
            return "Fútbol";
        }
        List<String> names = new ArrayList<>();
        for (Integer sId : sportsIds) {
            if (sId != null && sportsMap.containsKey(sId)) {
                names.add(sportsMap.get(sId));
            }
        }
        return names.isEmpty() ? "Fútbol" : String.join(", ", names);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventTypeBookingCountDto> getBookingsCountByEventType(UUID venueId, Integer status, LocalDate startDate, LocalDate endDate) {
        log.info("[getBookingsCountByEventType] Generando reporte de reservas por tipo de evento. Filtros: venueId={}, status={}, startDate={}, endDate={}",
                venueId, status, startDate, endDate);

        // 1. Obtener catálogo de tipos de evento (idParamParent = 1)
        List<com.sportsbooking.mainapi.dto.parameter.ParameterDto> eventTypeParams = new ArrayList<>();
        try {
            List<com.sportsbooking.mainapi.dto.parameter.ParameterDto> params = parameterService.getByParent(1);
            if (params != null) {
                eventTypeParams.addAll(params);
            }
        } catch (Exception e) {
            log.warn("[getBookingsCountByEventType] No se pudieron cargar los parámetros de tipos de evento: {}", e.getMessage());
        }

        // 2. Obtener reservas
        List<BookingEntity> bookings = bookingRepository.findAll();

        // Aplicar filtros opcionales
        java.util.stream.Stream<BookingEntity> stream = bookings.stream();
        if (venueId != null) {
            stream = stream.filter(b -> venueId.equals(b.getVenueId()));
        }
        if (status != null) {
            stream = stream.filter(b -> b.getStatus() == status);
        }
        if (startDate != null) {
            stream = stream.filter(b -> b.getBookingDate() != null && !b.getBookingDate().isBefore(startDate));
        }
        if (endDate != null) {
            stream = stream.filter(b -> b.getBookingDate() != null && !b.getBookingDate().isAfter(endDate));
        }

        List<BookingEntity> filteredBookings = stream.toList();
        long totalBookings = filteredBookings.size();

        // 3. Contabilizar reservas por tipo de evento
        // Si una reserva contiene múltiples tipos de eventos (ej: [1, 2]), suma 1 a cada tipo
        Map<Integer, Long> countByEventType = new HashMap<>();
        for (BookingEntity b : filteredBookings) {
            Integer[] sports = b.getSportsId();
            if (sports != null && sports.length > 0) {
                Set<Integer> uniqueTypesInBooking = Arrays.stream(sports)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                for (Integer typeId : uniqueTypesInBooking) {
                    countByEventType.merge(typeId, 1L, Long::sum);
                }
            }
        }

        // 4. Mapear cada tipo de evento del catálogo con su conteo
        Map<Integer, EventTypeBookingCountDto> resultMap = new LinkedHashMap<>();
        for (var p : eventTypeParams) {
            if (p.getIdParam() == null) continue;
            long count = countByEventType.getOrDefault(p.getIdParam(), 0L);
            double percentage = totalBookings > 0
                    ? Math.round((count * 100.0 / totalBookings) * 100.0) / 100.0
                    : 0.0;

            resultMap.put(p.getIdParam(), EventTypeBookingCountDto.builder()
                    .eventTypeId(p.getIdParam())
                    .eventTypeName(p.getChildName() != null ? p.getChildName() : "Tipo #" + p.getIdParam())
                    .description(p.getDescription())
                    .icon(p.getIcon())
                    .iconType(p.getIconType())
                    .bookingCount(count)
                    .percentage(percentage)
                    .build());
        }

        // 5. Incluir cualquier id presente en las reservas que no esté en el catálogo activo
        for (Map.Entry<Integer, Long> entry : countByEventType.entrySet()) {
            Integer typeId = entry.getKey();
            if (!resultMap.containsKey(typeId)) {
                long count = entry.getValue();
                double percentage = totalBookings > 0
                        ? Math.round((count * 100.0 / totalBookings) * 100.0) / 100.0
                        : 0.0;
                resultMap.put(typeId, EventTypeBookingCountDto.builder()
                        .eventTypeId(typeId)
                        .eventTypeName("Tipo #" + typeId)
                        .description(null)
                        .icon(null)
                        .iconType(null)
                        .bookingCount(count)
                        .percentage(percentage)
                        .build());
            }
        }

        // Retornar lista ordenada descendentemente por cantidad de reservas
        return resultMap.values().stream()
                .sorted(Comparator.comparingLong(EventTypeBookingCountDto::getBookingCount).reversed())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getAdminEventTypesCount() {
        log.info("[getAdminEventTypesCount] Calculando el total de tipos de eventos de reservas para administración de todos los venues");

        // 1. Cargar tipos de eventos del catálogo (idParamParent = 1)
        List<com.sportsbooking.mainapi.dto.parameter.ParameterDto> eventTypeParams = new ArrayList<>();
        Map<Integer, String> sportsMap = new HashMap<>();
        try {
            List<com.sportsbooking.mainapi.dto.parameter.ParameterDto> params = parameterService.getByParent(1);
            if (params != null) {
                eventTypeParams.addAll(params);
                for (var p : params) {
                    if (p.getIdParam() != null && p.getChildName() != null) {
                        sportsMap.put(p.getIdParam(), p.getChildName());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[getAdminEventTypesCount] Error al cargar parámetros de tipos de evento: {}", e.getMessage());
        }

        // Inicializar mapa preservando tipos de evento del catálogo con 0 reservas
        Map<String, Long> counts = new LinkedHashMap<>();
        for (var p : eventTypeParams) {
            if (p.getChildName() != null && !p.getChildName().isBlank()) {
                counts.put(p.getChildName(), 0L);
            }
        }

        // 2. Obtener todas las reservas de todos los venues en general
        List<BookingEntity> allBookings = bookingRepository.findAll();

        // 3. Contabilizar cada tipo de evento presente en cada reserva
        // Si una reserva tiene varios eventos (ej: [1, 2]), suma 1 para cada tipo de evento
        for (BookingEntity b : allBookings) {
            Integer[] sports = b.getSportsId();
            if (sports != null && sports.length > 0) {
                Set<Integer> uniqueTypes = Arrays.stream(sports)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                for (Integer sId : uniqueTypes) {
                    String eventName = sportsMap.get(sId);
                    if (eventName == null || eventName.isBlank()) {
                        eventName = "Tipo #" + sId;
                    }
                    counts.merge(eventName, 1L, Long::sum);
                }
            }
        }

        // 4. Retornar ordenado descendentemente por mayor cantidad de reservas
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }


    @Data

    private static class HoldMetadata {
        private UUID userId;
        private UUID venueId;
        private LocalDate date;
        private List<SlotItem> slots;
        private int extendsCount = 0;

        public HoldMetadata() {
        }

        public HoldMetadata(UUID userId, UUID venueId, LocalDate date, List<SlotItem> slots) {
            this.userId = userId;
            this.venueId = venueId;
            this.date = date;
            this.slots = slots;
        }
    }
}
