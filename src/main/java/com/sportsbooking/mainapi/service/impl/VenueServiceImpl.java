package com.sportsbooking.mainapi.service.impl;

import com.sportsbooking.mainapi.common.exception.ResourceNotFoundException;
import com.sportsbooking.mainapi.config.VenueProperties;
import com.sportsbooking.mainapi.dto.common.MessageResponse;
import com.sportsbooking.mainapi.dto.venue.VenueListItemDto;
import com.sportsbooking.mainapi.dto.venue.request.CreateVenueRequest;
import com.sportsbooking.mainapi.dto.venue.request.InBoundsRequest;
import com.sportsbooking.mainapi.dto.venue.request.SearchVenuesRequest;
import com.sportsbooking.mainapi.dto.venue.request.UpdateVenueRequest;
import com.sportsbooking.mainapi.dto.venue.response.VenueBookingReportDto;
import com.sportsbooking.mainapi.dto.venue.response.VenueStatsResponseDto;
import com.sportsbooking.mainapi.dto.venue.response.VenueDetailedReportDto;
import com.sportsbooking.mainapi.dto.venue.response.VenueDetailResponse;
import com.sportsbooking.mainapi.dto.venue.response.HostVenueResponseDto;
import com.sportsbooking.mainapi.enums.ScheduleStatus;
import com.sportsbooking.mainapi.enums.ScheduleType;
import com.sportsbooking.mainapi.jpa.entity.*;
import com.sportsbooking.mainapi.jpa.entity.enums.HostDebtStatus;
import com.sportsbooking.mainapi.jpa.entity.enums.VenueStatus;
import com.sportsbooking.mainapi.jpa.myrepository.VenueMyRepository;
import com.sportsbooking.mainapi.jpa.repository.*;
import com.sportsbooking.mainapi.service.FileStorageService;
import com.sportsbooking.mainapi.service.UbigeoService;
import com.sportsbooking.mainapi.service.VenueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import com.sportsbooking.mainapi.enums.AppDayOfWeek;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de venues.
 *
 * <p>
 * Usa JPA para CRUD simple y MyBatis para queries complejas (PostGIS,
 * búsqueda).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VenueServiceImpl implements VenueService {

    private final VenueRepository venueRepository;
    private final AvailableVenueRepository availableVenueRepository;
    private final VenuePayBankRepository payBankRepository;
    private final VenuePayWalletRepository payWalletRepository;
    private final VenueParkingRepository venueParkingRepository;
    private final VenueBathroomRepository venueBathroomRepository;
    private final VenueReviewRepository reviewRepository;
    private final VenuePhotoRepository photoRepository;
    private final VenueMyRepository venueMyRepository;
    private final VenueProperties venueProperties;
    private final FileStorageService fileStorageService;
    private final UbigeoService ubigeoService;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final HostDebtsRepository hostDebtsRepository;
    private final ParameterRepository parameterRepository;

    /** Factory para crear geometrías JTS con SRID 4326 (WGS84). */
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    // ─────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public VenueDetailResponse createVenue(UUID hostUserId, CreateVenueRequest request) {
        log.info("[Venue] Creando venue '{}' para host {}", request.name(), hostUserId);

        // 1. Crear la entidad venue
        VenueEntity venue = VenueEntity.builder()
                .hostUserId(hostUserId)
                .name(request.name())
                .address(request.address())
                .location(createPoint(request.latitude(), request.longitude()))
                .ubigeo(request.ubigeo())
                .description(request.description())
                .dimensions(request.dimensions())
                .isIlluminated(request.isIlluminated())
                .contactNumber(request.contactNumber())
                .idSports(toArray(request.idSports()))
                .idSurfaces(toArray(request.idSurfaces()))
                .idComforts(toArray(request.idComforts()))
                // --- NUEVOS CAMPOS ---
                .minCapacity(request.minCapacity())
                .maxCapacity(request.maxCapacity())
                .banquetCapacity(request.banquetCapacity())
                .theaterCapacity(request.theaterCapacity())
                .cocktail_capacity(request.cocktailCapacity())
                .bathroomsCount(request.bathroomsCount())
                .idAmenities(toArray(request.idAmenities()))
                .idFurniture(toArray(request.idFurniture()))
                .idClimate(toArray(request.idClimate()))
                .idAudioVideo(toArray(request.idAudioVideo()))
                .idLighting(toArray(request.idLighting()))
                .idFacilities(toArray(request.idFacilities()))
                .idAccessibility(toArray(request.idAccessibility()))
                .idServices(toArray(request.idServices()))
                .basePrice(request.basePrice())
                .pricingStartTime(request.pricingStartTime())
                .pricingEndTime(request.pricingEndTime())
                .build();

        venue = venueRepository.save(venue);
        final UUID venueId = venue.getId();

        // 2. Crear horarios
        if (request.schedules() != null) {
            validateCreateSchedules(request.schedules());
            List<AvailableVenueEntity> schedules = request.schedules().stream()
                    .map(s -> {
                        ScheduleStatus st = s.status() != null ? s.status() : ScheduleStatus.ACTIVE;
                        LocalTime defaultStart = s.scheduleType() == ScheduleType.NIGHT ? LocalTime.of(18, 0)
                                : LocalTime.of(6, 0);
                        LocalTime defaultEnd = s.scheduleType() == ScheduleType.NIGHT ? LocalTime.of(23, 0)
                                : LocalTime.of(18, 0);
                        Integer[] allDays = Arrays.stream(AppDayOfWeek.values()).map(AppDayOfWeek::getValue)
                                .toArray(Integer[]::new);
                        return AvailableVenueEntity.builder()
                                .venueId(venueId)
                                .scheduleType(s.scheduleType().getValue())
                                .status(st.getValue())
                                .startTime(st == ScheduleStatus.ACTIVE ? s.startTime() : defaultStart)
                                .endTime(st == ScheduleStatus.ACTIVE ? s.endTime() : defaultEnd)
                                .price(st == ScheduleStatus.ACTIVE ? s.price() : BigDecimal.ZERO)
                                .days(st == ScheduleStatus.ACTIVE ? toArrayDays(s.days()) : allDays)
                                .minHours(s.minHours())
                                .maxHours(s.maxHours())
                                .extraHourPrice(s.extraHourPrice())
                                .minAdvanceDays(s.minAdvanceDays())
                                .cancellationPolicy(s.cancellationPolicy())
                                .build();
                    })
                    .toList();
            availableVenueRepository.saveAll(schedules);
        }

        // 3. Crear cuentas bancarias
        if (request.payBanks() != null) {
            List<VenuePayBankEntity> banks = request.payBanks().stream()
                    .map(b -> VenuePayBankEntity.builder()
                            .venueId(venueId)
                            .idBank(b.idBank())
                            .nombreTitular(b.nombreTitular())
                            .numeroCuenta(b.numeroCuenta())
                            .cci(b.cci())
                            .build())
                    .toList();
            payBankRepository.saveAll(banks);
        }

        // 4. Crear billeteras digitales
        if (request.payWallets() != null) {
            List<VenuePayWalletEntity> wallets = request.payWallets().stream()
                    .map(w -> VenuePayWalletEntity.builder()
                            .venueId(venueId)
                            .idDigitalWallet(w.idDigitalWallet())
                            .nombreTitular(w.nombreTitular())
                            .numeroCelular(w.numeroCelular())
                            .build())
                    .toList();
            payWalletRepository.saveAll(wallets);
        }

        // 5. Crear parqueos
        if (request.parkings() != null) {
            List<VenueParkingEntity> parkings = request.parkings().stream()
                    .map(p -> VenueParkingEntity.builder()
                            .venueId(venueId)
                            .capacity(p.capacity())
                            .price(p.price())
                            .build())
                    .toList();
            venueParkingRepository.saveAll(parkings);
        }

        // 6. Crear baños
        if (request.bathrooms() != null) {
            List<VenueBathroomEntity> bathrooms = request.bathrooms().stream()
                    .map(b -> VenueBathroomEntity.builder()
                            .venueId(venueId)
                            .amount(b.amount())
                            .build())
                    .toList();
            venueBathroomRepository.saveAll(bathrooms);
        }

        log.info("[Venue] Venue '{}' creado con ID {}", request.name(), venueId);
        return getVenueById(venueId);
    }

    // ─────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public VenueDetailResponse getVenueById(UUID venueId) {
        VenueEntity venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", venueId));

        if (venue.getStatus() != VenueStatus.ACTIVE) {
            throw new ResourceNotFoundException("Venue (Inactivo / Eliminado)", venueId);
        }

        List<AvailableVenueEntity> schedules = availableVenueRepository.findByVenueId(venueId);
        List<VenuePayBankEntity> banks = payBankRepository.findByVenueId(venueId);
        List<VenuePayWalletEntity> wallets = payWalletRepository.findByVenueId(venueId);
        List<VenueParkingEntity> parkings = venueParkingRepository.findByVenueId(venueId);
        List<VenueBathroomEntity> bathrooms = venueBathroomRepository.findByVenueId(venueId);
        List<VenuePhotoEntity> photos = photoRepository.findByVenueId(venueId);
        double avgRating = reviewRepository.avgPointsByVenueId(venueId);
        long reviewCount = reviewRepository.countByVenueId(venueId);

        return toDetailResponse(venue, schedules, banks, wallets, parkings, bathrooms, photos, avgRating, reviewCount);
    }

    // ─────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public VenueDetailResponse updateVenue(UUID hostUserId, UUID venueId, UpdateVenueRequest request) {
        VenueEntity venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", venueId));

        validateOwnership(venue, hostUserId);

        // Actualizar solo los campos presentes
        if (request.name() != null)
            venue.setName(request.name());
        if (request.address() != null)
            venue.setAddress(request.address());
        if (request.ubigeo() != null)
            venue.setUbigeo(request.ubigeo());
        if (request.description() != null)
            venue.setDescription(request.description());
        if (request.dimensions() != null)
            venue.setDimensions(request.dimensions());
        if (request.isIlluminated() != null)
            venue.setIlluminated(request.isIlluminated());
        if (request.contactNumber() != null)
            venue.setContactNumber(request.contactNumber());
        if (request.idSports() != null)
            venue.setIdSports(toArray(request.idSports()));
        if (request.idSurfaces() != null)
            venue.setIdSurfaces(toArray(request.idSurfaces()));
        if (request.idComforts() != null)
            venue.setIdComforts(toArray(request.idComforts()));

        // --- NUEVOS CAMPOS ---
        if (request.minCapacity() != null)
            venue.setMinCapacity(request.minCapacity());
        if (request.maxCapacity() != null)
            venue.setMaxCapacity(request.maxCapacity());
        if (request.banquetCapacity() != null)
            venue.setBanquetCapacity(request.banquetCapacity());
        if (request.theaterCapacity() != null)
            venue.setTheaterCapacity(request.theaterCapacity());
        if (request.cocktailCapacity() != null)
            venue.setCocktail_capacity(request.cocktailCapacity());
        if (request.bathroomsCount() != null)
            venue.setBathroomsCount(request.bathroomsCount());
        if (request.idAmenities() != null)
            venue.setIdAmenities(toArray(request.idAmenities()));
        if (request.idFurniture() != null)
            venue.setIdFurniture(toArray(request.idFurniture()));
        if (request.idClimate() != null)
            venue.setIdClimate(toArray(request.idClimate()));
        if (request.idAudioVideo() != null)
            venue.setIdAudioVideo(toArray(request.idAudioVideo()));
        if (request.idLighting() != null)
            venue.setIdLighting(toArray(request.idLighting()));
        if (request.idFacilities() != null)
            venue.setIdFacilities(toArray(request.idFacilities()));
        if (request.idAccessibility() != null)
            venue.setIdAccessibility(toArray(request.idAccessibility()));
        if (request.idServices() != null)
            venue.setIdServices(toArray(request.idServices()));
        if (request.basePrice() != null)
            venue.setBasePrice(request.basePrice());
        if (request.pricingStartTime() != null)
            venue.setPricingStartTime(request.pricingStartTime());
        if (request.pricingEndTime() != null)
            venue.setPricingEndTime(request.pricingEndTime());

        if (request.latitude() != null && request.longitude() != null) {
            venue.setLocation(createPoint(request.latitude(), request.longitude()));
        }

        venueRepository.save(venue);

        // Reemplazar horarios si se envían
        if (request.schedules() != null) {
            validateUpdateSchedules(request.schedules());
            availableVenueRepository.deleteByVenueId(venueId);
            List<AvailableVenueEntity> schedules = request.schedules().stream()
                    .map(s -> {
                        ScheduleStatus st = s.status() != null ? s.status() : ScheduleStatus.ACTIVE;
                        LocalTime defaultStart = s.scheduleType() == ScheduleType.NIGHT ? LocalTime.of(18, 0)
                                : LocalTime.of(6, 0);
                        LocalTime defaultEnd = s.scheduleType() == ScheduleType.NIGHT ? LocalTime.of(23, 0)
                                : LocalTime.of(18, 0);
                        Integer[] allDays = Arrays.stream(AppDayOfWeek.values()).map(AppDayOfWeek::getValue)
                                .toArray(Integer[]::new);
                        return AvailableVenueEntity.builder()
                                .venueId(venueId)
                                .scheduleType(s.scheduleType().getValue())
                                .status(st.getValue())
                                .startTime(st == ScheduleStatus.ACTIVE ? s.startTime() : defaultStart)
                                .endTime(st == ScheduleStatus.ACTIVE ? s.endTime() : defaultEnd)
                                .price(st == ScheduleStatus.ACTIVE ? s.price() : BigDecimal.ZERO)
                                .days(st == ScheduleStatus.ACTIVE ? toArrayDays(s.days()) : allDays)
                                .minHours(s.minHours() != null ? s.minHours() : request.minHours())
                                .maxHours(s.maxHours() != null ? s.maxHours() : request.maxHours())
                                .extraHourPrice(
                                        s.extraHourPrice() != null ? s.extraHourPrice() : request.extraHourPrice())
                                .minAdvanceDays(
                                        s.minAdvanceDays() != null ? s.minAdvanceDays() : request.minAdvanceDays())
                                .cancellationPolicy(s.cancellationPolicy() != null ? s.cancellationPolicy()
                                        : request.cancellationPolicy())
                                .build();
                    })
                    .toList();
            availableVenueRepository.saveAll(schedules);
        }

        // Reemplazar cuentas bancarias si se envían
        if (request.payBanks() != null) {
            payBankRepository.deleteByVenueId(venueId);
            List<VenuePayBankEntity> banks = request.payBanks().stream()
                    .map(b -> VenuePayBankEntity.builder()
                            .venueId(venueId)
                            .idBank(b.idBank())
                            .nombreTitular(b.nombreTitular())
                            .numeroCuenta(b.numeroCuenta())
                            .cci(b.cci())
                            .build())
                    .toList();
            payBankRepository.saveAll(banks);
        }

        // Reemplazar billeteras si se envían
        if (request.payWallets() != null) {
            payWalletRepository.deleteByVenueId(venueId);
            List<VenuePayWalletEntity> wallets = request.payWallets().stream()
                    .map(w -> VenuePayWalletEntity.builder()
                            .venueId(venueId)
                            .idDigitalWallet(w.idDigitalWallet())
                            .nombreTitular(w.nombreTitular())
                            .numeroCelular(w.numeroCelular())
                            .build())
                    .toList();
            payWalletRepository.saveAll(wallets);
        }

        // Reemplazar parqueos si se envían
        if (request.parkings() != null) {
            venueParkingRepository.deleteByVenueId(venueId);
            List<VenueParkingEntity> parkings = request.parkings().stream()
                    .map(p -> VenueParkingEntity.builder()
                            .venueId(venueId)
                            .capacity(p.capacity())
                            .price(p.price())
                            .build())
                    .toList();
            venueParkingRepository.saveAll(parkings);
        }

        // Reemplazar baños si se envían
        if (request.bathrooms() != null) {
            venueBathroomRepository.deleteByVenueId(venueId);
            List<VenueBathroomEntity> bathrooms = request.bathrooms().stream()
                    .map(b -> VenueBathroomEntity.builder()
                            .venueId(venueId)
                            .amount(b.amount())
                            .build())
                    .toList();
            venueBathroomRepository.saveAll(bathrooms);
        }

        log.info("[Venue] Venue {} actualizado por host {}", venueId, hostUserId);
        return getVenueById(venueId);
    }

    // ─────────────────────────────────────────────
    // DELETE (soft)
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public MessageResponse deleteVenue(UUID hostUserId, UUID venueId) {
        VenueEntity venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", venueId));

        validateOwnership(venue, hostUserId);

        venue.setStatus(VenueStatus.INACTIVE);
        venueRepository.save(venue);

        log.info("[Venue] Venue {} desactivado por host {}", venueId, hostUserId);
        return new MessageResponse("Venue eliminado correctamente");
    }

    // ─────────────────────────────────────────────
    // SEARCH (MyBatis + PostGIS)
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<VenueListItemDto> findVenuesInBounds(InBoundsRequest request) {
        int maxMeters = venueProperties.getMaxSearchRadiusMeters();
        int maxKm = maxMeters / 1000;

        if (request.radiusKm() != null) {
            if (request.radiusKm() < 1 || request.radiusKm() > maxKm) {
                throw new IllegalArgumentException("El radio de búsqueda debe estar entre 1km y " + maxKm + "km.");
            }
        }

        int radiusMeters = (request.radiusKm() != null)
                ? request.radiusKm() * 1000
                : 10000; // Por defecto 10km si no envía

        List<VenueListItemDto> list = venueMyRepository.findInBounds(
                request.lat(),
                request.lng(),
                radiusMeters,
                request.sportId(),
                request.venueName(),
                request.surfaceId(),
                request.hasLighting(),
                request.minPrice(),
                request.maxPrice());
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VenueListItemDto> searchVenues(SearchVenuesRequest request) {
        Integer radiusMeters = null;
        if (request.latitude() != null && request.longitude() != null) {
            int maxMeters = venueProperties.getMaxSearchRadiusMeters();
            radiusMeters = (request.radiusKm() != null)
                    ? Math.min(request.radiusKm() * 1000, maxMeters)
                    : maxMeters;
        }

        Integer[] surfaceIds = request.surfaceIds() != null
                ? request.surfaceIds().toArray(Integer[]::new)
                : null;

        List<VenueListItemDto> list = venueMyRepository.searchVenues(
                request.sportId(),
                surfaceIds,
                request.isIlluminated(),
                request.keyword(),
                request.latitude(),
                request.longitude(),
                radiusMeters);
        return list;
    }

    // ─────────────────────────────────────────────
    // PHOTOS
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public MessageResponse uploadPhotos(UUID userId, UUID venueId, MultipartFile[] files) {
        VenueEntity venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", venueId));

        validateOwnership(venue, userId);

        // Validar cantidad total (existentes + nuevas)
        int maxCount = venueProperties.getImages().getMaxCount();
        long existingCount = photoRepository.countByVenueId(venueId);
        if (existingCount + files.length > maxCount) {
            throw new IllegalArgumentException(
                    "Máximo " + maxCount + " fotos. Actuales: " + existingCount + ", nuevas: " + files.length);
        }

        int saved = 0;
        for (MultipartFile file : files) {
            String filename = fileStorageService.store(file, venueId);

            VenuePhotoEntity photo = VenuePhotoEntity.builder()
                    .venueId(venueId)
                    .url(filename)
                    .build();
            photoRepository.save(photo);
            saved++;
        }

        log.info("[Venue] {} fotos subidas al venue {} por user {}", saved, venueId, userId);
        return new MessageResponse(saved + " foto(s) subida(s) correctamente");
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> getPhotoResource(UUID photoId) {
        VenuePhotoEntity photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("VenuePhoto", photoId));

        Resource resource = fileStorageService.load(photo.getVenueId(), photo.getUrl());

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + photo.getUrl() + "\"")
                .body(resource);
    }

    @Override
    @Transactional
    public MessageResponse deletePhoto(UUID userId, UUID photoId) {
        VenuePhotoEntity photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("VenuePhoto", photoId));

        VenueEntity venue = venueRepository.findById(photo.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue", photo.getVenueId()));

        validateOwnership(venue, userId);

        // 1. Borrar archivo físico
        fileStorageService.delete(photo.getVenueId(), photo.getUrl());

        // 2. Borrar de BD
        photoRepository.delete(photo);

        log.info("[Venue] Foto {} eliminada del venue {} por user {}", photoId, photo.getVenueId(), userId);
        return new MessageResponse("Foto eliminada correctamente");
    }

    // ─────────────────────────────────────────────
    // Métodos privados
    // ─────────────────────────────────────────────

    private void validateCreateSchedules(List<CreateVenueRequest.AvailableVenueItem> schedules) {
        for (var s : schedules) {
            ScheduleStatus status = s.status() != null ? s.status() : ScheduleStatus.ACTIVE;
            if (status == ScheduleStatus.ACTIVE) {
                if (s.startTime() == null || s.endTime() == null || s.price() == null || s.days() == null
                        || s.days().isEmpty()) {
                    throw new IllegalArgumentException(
                            "Si el horario está activo, debe completar horas, precio y días");
                }
                if (!s.startTime().isBefore(s.endTime())) {
                    throw new IllegalArgumentException("La hora de inicio debe ser menor a la hora de fin");
                }
                if (s.scheduleType() == ScheduleType.DAY) {
                    if (s.startTime().isBefore(LocalTime.of(6, 0)) || s.endTime().isAfter(LocalTime.of(18, 0))) {
                        throw new IllegalArgumentException("Horario diurno debe estar entre 06:00 y 18:00");
                    }
                } else if (s.scheduleType() == ScheduleType.NIGHT) {
                    if (s.startTime().isBefore(LocalTime.of(18, 0)) || s.endTime().isAfter(LocalTime.of(23, 0))) {
                        throw new IllegalArgumentException("Horario nocturno debe estar entre 18:00 y 23:00");
                    }
                }
            }
        }
    }

    private void validateUpdateSchedules(List<UpdateVenueRequest.AvailableVenueItem> schedules) {
        for (var s : schedules) {
            ScheduleStatus status = s.status() != null ? s.status() : ScheduleStatus.ACTIVE;
            if (status == ScheduleStatus.ACTIVE) {
                if (s.startTime() == null || s.endTime() == null || s.price() == null || s.days() == null
                        || s.days().isEmpty()) {
                    throw new IllegalArgumentException(
                            "Si el horario está activo, debe completar horas, precio y días");
                }
                if (!s.startTime().isBefore(s.endTime())) {
                    throw new IllegalArgumentException("La hora de inicio debe ser menor a la hora de fin");
                }
                if (s.scheduleType() == ScheduleType.DAY) {
                    if (s.startTime().isBefore(LocalTime.of(6, 0)) || s.endTime().isAfter(LocalTime.of(18, 0))) {
                        throw new IllegalArgumentException("Horario diurno debe estar entre 06:00 y 18:00");
                    }
                } else if (s.scheduleType() == ScheduleType.NIGHT) {
                    if (s.startTime().isBefore(LocalTime.of(18, 0)) || s.endTime().isAfter(LocalTime.of(23, 0))) {
                        throw new IllegalArgumentException("Horario nocturno debe estar entre 18:00 y 23:00");
                    }
                }
            }
        }
    }

    /**
     * Valida que el usuario sea el propietario del venue.
     *
     * @throws AccessDeniedException si no es el propietario.
     */
    private void validateOwnership(VenueEntity venue, UUID hostUserId) {
        if (!venue.getHostUserId().equals(hostUserId)) {
            throw new AccessDeniedException("No tiene permisos para modificar este venue");
        }
    }

    /**
     * Crea un {@link Point} JTS con SRID 4326 (WGS84).
     * Note: JTS usa (x=longitude, y=latitude).
     */
    private Point createPoint(double latitude, double longitude) {
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
        point.setSRID(4326);
        return point;
    }

    /** Convierte una lista a array (para PostgreSQL integer[]). */
    private Integer[] toArray(List<Integer> list) {
        return list == null ? new Integer[0] : list.toArray(new Integer[0]);
    }

    private Integer[] toArrayDays(List<AppDayOfWeek> list) {
        return list == null ? new Integer[0] : list.stream().map(AppDayOfWeek::getValue).toArray(Integer[]::new);
    }

    private List<Integer> toListSafe(Integer[] array) {
        return array == null ? java.util.List.of() : Arrays.asList(array);
    }

    /** Mapea entidades JPA a la respuesta de detalle. */
    private VenueDetailResponse toDetailResponse(
            VenueEntity venue,
            List<AvailableVenueEntity> availSchedules,
            List<VenuePayBankEntity> banks,
            List<VenuePayWalletEntity> wallets,
            List<VenueParkingEntity> parkings,
            List<VenueBathroomEntity> bathrooms,
            List<VenuePhotoEntity> photos,
            double avgRating,
            long reviewCount) {
        List<VenueDetailResponse.ScheduleItem> scheduleItems = availSchedules.stream()
                .map(this::toScheduleItem)
                .toList();

        return new VenueDetailResponse(
                venue.getId(),
                venue.getHostUserId(),
                venue.getName(),
                venue.getAddress(),
                venue.getLocation().getY(), // latitude
                venue.getLocation().getX(), // longitude
                ubigeoService.getUbigeoText(venue.getUbigeo()),
                venue.getUbigeo(), // ubigeoCode
                venue.getDescription(),
                venue.getDimensions(),
                venue.isIlluminated(),
                toListSafe(venue.getIdSports()),
                toListSafe(venue.getIdSurfaces()),
                toListSafe(venue.getIdComforts()),
                venue.getStatus() != null ? venue.getStatus().getValue() : 1,
                venue.getContactNumber(),
                venue.getCreatedAt(),
                scheduleItems,
                banks.stream().map(b -> new VenueDetailResponse.PayBankItem(
                        b.getId(), b.getIdBank(),
                        b.getNombreTitular(), b.getNumeroCuenta(), b.getCci())).toList(),
                wallets.stream().map(w -> new VenueDetailResponse.PayWalletItem(
                        w.getId(), w.getIdDigitalWallet(),
                        w.getNombreTitular(), w.getNumeroCelular())).toList(),
                parkings.stream().map(p -> new VenueDetailResponse.VenueParkingItem(
                        p.getId(), p.getCapacity(), p.getPrice())).toList(),
                bathrooms.stream().map(b -> new VenueDetailResponse.VenueBathroomItem(
                        b.getId(), b.getAmount())).toList(),
                photos.stream().map(p -> new VenueDetailResponse.PhotoItem(
                        p.getId(), p.getUrl(), p.getCreatedAt())).toList(),
                avgRating,
                reviewCount,
                // --- NUEVOS CAMPOS ---
                venue.getMinCapacity(),
                venue.getMaxCapacity(),
                venue.getBanquetCapacity(),
                venue.getTheaterCapacity(),
                venue.getCocktail_capacity(),
                venue.getBathroomsCount(),
                toListSafe(venue.getIdAmenities()),
                toListSafe(venue.getIdFurniture()),
                toListSafe(venue.getIdClimate()),
                toListSafe(venue.getIdAudioVideo()),
                toListSafe(venue.getIdLighting()),
                toListSafe(venue.getIdFacilities()),
                toListSafe(venue.getIdAccessibility()),
                toListSafe(venue.getIdServices()),
                venue.getBasePrice(),
                venue.getPricingStartTime(),
                venue.getPricingEndTime());
    }

    private VenueDetailResponse.ScheduleItem toScheduleItem(AvailableVenueEntity s) {
        return new VenueDetailResponse.ScheduleItem(
                s.getId(),
                ScheduleType.fromValue(s.getScheduleType()),
                ScheduleStatus.fromValue(s.getStatus()),
                s.getStartTime(),
                s.getEndTime(),
                s.getPrice(),
                s.getDays() != null ? Arrays.stream(s.getDays()).map(AppDayOfWeek::fromValue).toList()
                        : java.util.List.of(),
                // --- NUEVOS CAMPOS ---
                s.getMinHours(),
                s.getMaxHours(),
                s.getExtraHourPrice(),
                s.getMinAdvanceDays(),
                s.getCancellationPolicy());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<VenueListItemDto> getMyVenues(UUID userId) {
        return venueMyRepository.findByHostUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VenueBookingReportDto> getVenuesBookingReport() {
        log.info("[Venue] Generando reporte de reservas y dueños para todos los venues");
        List<VenueEntity> venues = venueRepository.findAll();
        if (venues.isEmpty()) {
            return List.of();
        }

        List<UserEntity> users = userRepository.findAll();
        Map<UUID, String> ownerNames = users.stream()
                .filter(u -> u.getId() != null)
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getName, (a, b) -> a));

        List<BookingEntity> allBookings = bookingRepository.findAll();
        Map<UUID, Long> totalBookingsCount = allBookings.stream()
                .filter(b -> b.getVenueId() != null)
                .collect(Collectors.groupingBy(BookingEntity::getVenueId, Collectors.counting()));

        OffsetDateTime oneMonthAgo = OffsetDateTime.now(ZoneOffset.UTC).minusMonths(1);
        Map<UUID, Long> lastMonthBookingsCount = allBookings.stream()
                .filter(b -> b.getVenueId() != null && b.getCreatedAt() != null && b.getCreatedAt().isAfter(oneMonthAgo))
                .collect(Collectors.groupingBy(BookingEntity::getVenueId, Collectors.counting()));

        List<VenueReviewEntity> allReviews = reviewRepository.findAll();
        Map<UUID, List<VenueReviewEntity>> reviewsByVenue = allReviews.stream()
                .filter(r -> r.getVenueId() != null)
                .collect(Collectors.groupingBy(VenueReviewEntity::getVenueId));

        List<HostDebts> allDebts = hostDebtsRepository.findAll();
        Map<UUID, List<HostDebts>> debtsByVenue = allDebts.stream()
                .filter(d -> d.getVenueId() != null)
                .collect(Collectors.groupingBy(HostDebts::getVenueId));

        return venues.stream()
                .map(venue -> {
                    UUID venueId = venue.getId();
                    String ownerName = ownerNames.getOrDefault(venue.getHostUserId(), "Desconocido");
                    long bookingsCount = totalBookingsCount.getOrDefault(venueId, 0L);
                    long bookingsLastMonthCount = lastMonthBookingsCount.getOrDefault(venueId, 0L);

                    List<VenueReviewEntity> venueReviews = reviewsByVenue.getOrDefault(venueId, List.of());
                    long reviewsCount = venueReviews.size();
                    double reviewsAverage = reviewsCount > 0 ? venueReviews.stream()
                            .mapToInt(VenueReviewEntity::getPoints)
                            .average()
                            .orElse(0.0) : 0.0;

                    List<HostDebts> venueDebts = debtsByVenue.getOrDefault(venueId, List.of());
                    BigDecimal incomeLastPeriod = BigDecimal.ZERO;
                    BigDecimal commissionLastPeriod = BigDecimal.ZERO;

                    if (!venueDebts.isEmpty()) {
                        HostDebts latestDebt = venueDebts.stream()
                                .max((a, b) -> a.getCycleEnd().compareTo(b.getCycleEnd()))
                                .orElse(null);
                        if (latestDebt != null) {
                            incomeLastPeriod = latestDebt.getTotalIncome() != null ? latestDebt.getTotalIncome() : BigDecimal.ZERO;
                            commissionLastPeriod = latestDebt.getCommissionAmount() != null ? latestDebt.getCommissionAmount() : BigDecimal.ZERO;
                        }
                    }

                    Double latitude = venue.getLocation() != null ? venue.getLocation().getY() : null;
                    Double longitude = venue.getLocation() != null ? venue.getLocation().getX() : null;

                    return VenueBookingReportDto.builder()
                            .id(venueId)
                            .name(venue.getName())
                            .ownerName(ownerName)
                            .bookingsCount(bookingsCount)
                            .address(venue.getAddress())
                            .latitude(latitude)
                            .longitude(longitude)
                            .bookingsLastMonthCount(bookingsLastMonthCount)
                            .incomeLastPeriod(incomeLastPeriod)
                            .commissionLastPeriod(commissionLastPeriod)
                            .ubigeo(venue.getUbigeo())
                            .reviewsCount(reviewsCount)
                            .reviewsAverage(reviewsAverage)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VenueStatsResponseDto getVenueStats() {
        log.info("[Venue] Obteniendo estadísticas de salones y propietarios");
        return venueRepository.getVenueStats();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VenueDetailedReportDto> getVenuesDetailedReport() {
        log.info("[Venue] Generando reporte detallado de salones");
        List<VenueEntity> venues = venueRepository.findAll();
        if (venues.isEmpty()) {
            return List.of();
        }

        // 1. Obtener todos los usuarios y mapearlos en memoria
        List<UserEntity> users = userRepository.findAll();
        Map<UUID, UserEntity> userMap = users.stream()
                .filter(u -> u.getId() != null)
                .collect(Collectors.toMap(UserEntity::getId, Function.identity(), (a, b) -> a));

        // 2. Obtener todas las reservas y agruparlas por venueId
        List<BookingEntity> bookings = bookingRepository.findAll();
        Map<UUID, List<BookingEntity>> bookingsByVenue = bookings.stream()
                .filter(b -> b.getVenueId() != null)
                .collect(Collectors.groupingBy(BookingEntity::getVenueId));

        // 3. Obtener comisión del parámetro 57
        BigDecimal commissionRate = BigDecimal.valueOf(0.10); // default 10%
        try {
            Optional<ParameterEntity> commissionParamOpt = parameterRepository.findById(57);
            if (commissionParamOpt.isPresent() && commissionParamOpt.get().getValue() != null) {
                commissionRate = BigDecimal.valueOf(commissionParamOpt.get().getValue());
            }
        } catch (Exception e) {
            log.warn("No se pudo cargar el parámetro 57 (comisión), usando default 10%: {}", e.getMessage());
        }

        List<VenueDetailedReportDto> report = new ArrayList<>();

        for (VenueEntity venue : venues) {
            UUID venueId = venue.getId();
            UserEntity owner = userMap.get(venue.getHostUserId());

            String ownerName = owner != null ? owner.getName() : "Desconocido";
            String ownerEmail = owner != null ? owner.getEmail() : "Desconocido";
            OffsetDateTime ownerCreatedAt = owner != null ? owner.getCreatedAt() : null;

            String ubigeoText = ubigeoService.getUbigeoText(venue.getUbigeo());

            String operatingHours = "No especificado";
            if (venue.getPricingStartTime() != null && venue.getPricingEndTime() != null) {
                operatingHours = venue.getPricingStartTime().toString() + " - " + venue.getPricingEndTime().toString();
            }

            List<BookingEntity> venueBookings = bookingsByVenue.getOrDefault(venueId, List.of());
            long bookingsCount = venueBookings.size();

            // Filtrar reservas completadas/confirmadas para ingresos/comisiones/ganancias
            BigDecimal totalIncome = venueBookings.stream()
                    .filter(b -> b.getStatus() == 3 || b.getStatus() == 4) // CONFIRMED = 3, COMPLETED = 4
                    .map(BookingEntity::getTotalAmount)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalCommissions = totalIncome.multiply(commissionRate);
            BigDecimal totalEarnings = totalIncome.subtract(totalCommissions);

            LocalDate latestBookingDate = venueBookings.stream()
                    .map(BookingEntity::getBookingDate)
                    .filter(java.util.Objects::nonNull)
                    .max(LocalDate::compareTo)
                    .orElse(null);

             // Mapear reservas individuales detalladas
             List<VenueDetailedReportDto.BookingDetailReportDto> bookingDetails = new ArrayList<>();
             for (BookingEntity b : venueBookings) {
                 UserEntity client = userMap.get(b.getClientUserId());
                 String clientName = client != null ? client.getName() : "Desconocido";
                 String clientEmail = client != null ? client.getEmail() : "Desconocido";
 
                 String bookingHours = getBookingTimeSlotHelper(b.getDetails());
 
                 BigDecimal bookingAmount = b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO;
                 BigDecimal bookingCommission = BigDecimal.ZERO;
                 BigDecimal bookingEarnings = BigDecimal.ZERO;
 
                 if (b.getStatus() == 3 || b.getStatus() == 4) { // CONFIRMED = 3, COMPLETED = 4
                     bookingCommission = bookingAmount.multiply(commissionRate);
                     bookingEarnings = bookingAmount.subtract(bookingCommission);
                 }
 
                 bookingDetails.add(VenueDetailedReportDto.BookingDetailReportDto.builder()
                         .bookingId(b.getId())
                         .bookingDate(b.getBookingDate())
                         .createdAt(b.getCreatedAt())
                         .bookingHours(bookingHours)
                         .totalAmount(bookingAmount)
                         .commission(bookingCommission)
                         .earnings(bookingEarnings)
                         .status(b.getStatus())
                         .clientName(clientName)
                         .clientEmail(clientEmail)
                         .build());
             }

            report.add(VenueDetailedReportDto.builder()
                    .ownerName(ownerName)
                    .ownerEmail(ownerEmail)
                    .ownerCreatedAt(ownerCreatedAt)
                    .venueId(venueId)
                    .venueName(venue.getName())
                    .venueCreatedAt(venue.getCreatedAt())
                    .ubigeo(venue.getUbigeo())
                    .ubigeoText(ubigeoText)
                    .operatingHours(operatingHours)
                    .bookingsCount(bookingsCount)
                    .totalIncome(totalIncome)
                    .totalCommissions(totalCommissions)
                    .totalEarnings(totalEarnings)
                    .latestBookingDate(latestBookingDate)
                    .bookings(bookingDetails)
                    .build());
        }

        return report;
    }

    private String getBookingTimeSlotHelper(List<BookingDetailEntity> details) {
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

    @Override
    @Transactional(readOnly = true)
    public List<HostVenueResponseDto> getHostVenues(UUID hostUserId) {
        log.info("[Venue] Obteniendo salones para host ID: {}", hostUserId);

        if (!userRepository.existsById(hostUserId)) {
            throw new ResourceNotFoundException("Usuario", hostUserId);
        }

        List<VenueEntity> venues = venueRepository.findByHostUserId(hostUserId);

        return venues.stream().map(venue -> {
            List<String> eventTypes = List.of();
            if (venue.getIdSports() != null && venue.getIdSports().length > 0) {
                eventTypes = parameterRepository.findAllById(Arrays.asList(venue.getIdSports())).stream()
                        .map(ParameterEntity::getChildName)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());
            }

            double averageRating = reviewRepository.avgPointsByVenueId(venue.getId());
            long bookingsCount = bookingRepository.countByVenueId(venue.getId());
            BigDecimal totalEarnings = bookingRepository.sumEarningsByVenueId(venue.getId());

            String statusStr = (venue.getStatus() == VenueStatus.ACTIVE) ? "ACTIVO" : "INACTIVO";
            boolean isActive = (venue.getStatus() == VenueStatus.ACTIVE);

            return HostVenueResponseDto.builder()
                    .venueId(venue.getId())
                    .name(venue.getName())
                    .eventTypes(eventTypes)
                    .averageRating(averageRating)
                    .bookingsCount(bookingsCount)
                    .totalEarnings(totalEarnings)
                    .status(statusStr)
                    .isActive(isActive)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void disableVenuesPastDeadline() {
        log.info("[VenueStatusCheck] Iniciando verificación de deudas vencidas para inhabilitar salones...");
        OffsetDateTime now = OffsetDateTime.now();

        // 1. Obtener deudas no pagadas
        List<HostDebts> unpaidDebts = hostDebtsRepository.findByStatusNot(HostDebtStatus.PAID);
        
        int disabledCount = 0;
        for (HostDebts debt : unpaidDebts) {
            if (debt.getDeadline() != null && debt.getDeadline().isBefore(now)) {
                UUID venueId = debt.getVenueId();
                if (venueId != null) {
                    Optional<VenueEntity> venueOpt = venueRepository.findById(venueId);
                    if (venueOpt.isPresent()) {
                        VenueEntity venue = venueOpt.get();
                        
                        // Si el venue está activo, lo inhabilitamos automáticamente
                        if (venue.getStatus() == VenueStatus.ACTIVE) {
                            venue.setStatus(VenueStatus.INACTIVE);
                            venue.setDisabledReason(1); // 1 = inhabilitado automáticamente
                            venueRepository.save(venue);
                            disabledCount++;
                            log.info("[VenueStatusCheck] Venue '{}' (ID: {}) inhabilitado automáticamente debido a deuda vencida (Deadline: {})", 
                                    venue.getName(), venue.getId(), debt.getDeadline());
                        }
                    }
                }
            }
        }
        
        if (disabledCount > 0) {
            log.info("[VenueStatusCheck] Proceso finalizado. Se inhabilitaron {} salones.", disabledCount);
        } else {
            log.debug("[VenueStatusCheck] No se encontraron nuevos salones por inhabilitar.");
        }
    }
}
