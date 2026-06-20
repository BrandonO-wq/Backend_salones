package com.sportsbooking.mainapi.service.impl;

import com.sportsbooking.mainapi.common.exception.ResourceNotFoundException;
import com.sportsbooking.mainapi.config.VenueProperties;
import com.sportsbooking.mainapi.dto.common.MessageResponse;
import com.sportsbooking.mainapi.dto.venue.VenueListItemDto;
import com.sportsbooking.mainapi.dto.venue.request.CreateVenueRequest;
import com.sportsbooking.mainapi.dto.venue.request.InBoundsRequest;
import com.sportsbooking.mainapi.dto.venue.request.SearchVenuesRequest;
import com.sportsbooking.mainapi.dto.venue.request.UpdateVenueRequest;
import com.sportsbooking.mainapi.dto.venue.response.VenueDetailResponse;
import com.sportsbooking.mainapi.enums.ScheduleStatus;
import com.sportsbooking.mainapi.enums.ScheduleType;
import com.sportsbooking.mainapi.jpa.entity.*;
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
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import com.sportsbooking.mainapi.enums.AppDayOfWeek;
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
}
