package com.sportsbooking.mainapi.dto.venue.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonAlias;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import com.sportsbooking.mainapi.enums.ScheduleType;
import com.sportsbooking.mainapi.enums.ScheduleStatus;
import com.sportsbooking.mainapi.enums.AppDayOfWeek;

/**
 * Request para actualizar parcialmente un venue.
 *
 * <p>
 * Todos los campos son opcionales. Solo los presentes (no-null) se actualizan.
 */
public record UpdateVenueRequest(

                String name,

                String address,

                Double latitude,

                Double longitude,

                String ubigeo,

                String description,

                String dimensions,

                Boolean isIlluminated,

                String contactNumber,

                List<Integer> idSports,

                List<Integer> idSurfaces,

                List<Integer> idComforts,

                /** Si se envía, reemplaza todos los horarios existentes. */
                @Valid List<AvailableVenueItem> schedules,

                /** Si se envía, reemplaza todas las cuentas bancarias. */
                @Valid List<PayBankItem> payBanks,

                /** Si se envía, reemplaza todas las billeteras digitales. */
                @Valid List<PayWalletItem> payWallets,

                @Valid List<VenueParkingItem> parkings,

                @Valid List<VenueBathroomItem> bathrooms,

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
                Integer minHours,
                Integer maxHours,
                BigDecimal extraHourPrice,
                LocalTime pricingStartTime,
                LocalTime pricingEndTime,
                Integer minAdvanceDays,
                String cancellationPolicy) {

        public record AvailableVenueItem(
                        ScheduleType scheduleType,
                        ScheduleStatus status,
                        LocalTime startTime,
                        LocalTime endTime,
                        BigDecimal price,
                        List<AppDayOfWeek> days,
                        // Campos adicionales por-horario (opcionales)
                        Integer minHours,
                        Integer maxHours,
                        BigDecimal extraHourPrice,
                        Integer minAdvanceDays,
                        String cancellationPolicy) {
        }

        public record PayBankItem(
                        Integer idBank,
                        String nombreTitular,
                        String numeroCuenta,
                        String cci) {
        }

        public record PayWalletItem(
                        Integer idDigitalWallet,
                        String nombreTitular,
                        String numeroCelular) {
        }

        public record VenueParkingItem(
                        Integer capacity,
                        BigDecimal price) {
        }

        public record VenueBathroomItem(
                        @NotNull @JsonAlias("quantity") Integer amount) {
        }
}
