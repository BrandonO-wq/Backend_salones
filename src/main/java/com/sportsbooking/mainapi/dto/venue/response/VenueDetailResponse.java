package com.sportsbooking.mainapi.dto.venue.response;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.sportsbooking.mainapi.enums.ScheduleType;
import com.sportsbooking.mainapi.enums.ScheduleStatus;
import com.sportsbooking.mainapi.enums.AppDayOfWeek;
import java.util.UUID;

/**
 * Respuesta con el detalle completo de un venue.
 *
 * <p>Incluye horarios, datos de pago, fotos y rating.
 */
public record VenueDetailResponse(

        UUID id,
        UUID hostUserId,
        String name,
        String address,
        double latitude,
        double longitude,
        String ubigeo,
        String ubigeoCode,
        String description,
        String dimensions,
        boolean isIlluminated,
        List<Integer> idSports,
        List<Integer> idSurfaces,
        List<Integer> idComforts,
        int status,
        String contactNumber,
        OffsetDateTime createdAt,

        List<ScheduleItem> schedules,
        List<PayBankItem> payBanks,
        List<PayWalletItem> payWallets,
        List<VenueParkingItem> parkings,
        List<VenueBathroomItem> bathrooms,
        List<PhotoItem> photos,
        double avgRating,
        long reviewCount,

        // --- NUEVOS CAMPOS (SALONES DE EVENTOS) ---
        Integer minCapacity,
        Integer maxCapacity,
        Integer banquetCapacity,
        Integer theaterCapacity,
        Integer cocktailCapacity,
        Integer bathroomsCount,
        List<Integer> idAmenities,
        List<Integer> idFurniture,
        List<Integer> idClimate,
        List<Integer> idAudioVideo,
        List<Integer> idLighting,
        List<Integer> idFacilities,
        List<Integer> idAccessibility,
        List<Integer> idServices,
        BigDecimal basePrice,
        LocalTime pricingStartTime,
        LocalTime pricingEndTime
) {

    public record ScheduleItem(
            UUID id,
            ScheduleType scheduleType,
            ScheduleStatus status,
            LocalTime startTime,
            LocalTime endTime,
            BigDecimal price,
            List<AppDayOfWeek> days,
            // --- NUEVOS CAMPOS POR HORARIO ---
            Integer minHours,
            Integer maxHours,
            BigDecimal extraHourPrice,
            Integer minAdvanceDays,
            String cancellationPolicy
    ) {}

    public record PayBankItem(
            UUID id,
            int idBank,
            String nombreTitular,
            String numeroCuenta,
            String cci
    ) {}

    public record PayWalletItem(
            UUID id,
            int idDigitalWallet,
            String nombreTitular,
            String numeroCelular
    ) {}

    public record PhotoItem(
            UUID id,
            String url,
            OffsetDateTime createdAt
    ) {}

    public record VenueParkingItem(
            UUID id,
            Integer capacity,
            BigDecimal price
    ) {}

    public record VenueBathroomItem(
            UUID id,
            Integer amount
    ) {}
}
