package com.sportsbooking.mainapi.dto.venue.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import com.sportsbooking.mainapi.enums.ScheduleType;
import com.sportsbooking.mainapi.enums.ScheduleStatus;
import com.sportsbooking.mainapi.enums.AppDayOfWeek;

/**
 * Request para crear un venue completo con horarios y datos de pago.
 *
 * <p>
 * Incluye sub-objetos para horarios disponibles, cuentas bancarias
 * y billeteras digitales.
 */
public record CreateVenueRequest(

                @NotBlank(message = "El nombre es obligatorio") String name,

                @NotBlank(message = "La dirección es obligatoria") String address,

                @NotNull(message = "La latitud es obligatoria") Double latitude,

                @NotNull(message = "La longitud es obligatoria") Double longitude,

                String ubigeo,

                String description,

                String dimensions,

                boolean isIlluminated,

                String contactNumber,

                @NotEmpty(message = "Debe indicar al menos un deporte") List<Integer> idSports,

                List<Integer> idSurfaces,

                List<Integer> idComforts,

                /** Horarios disponibles del venue. */
                @Valid List<AvailableVenueItem> schedules,

                /** Cuentas bancarias. */
                @Valid List<PayBankItem> payBanks,

                /** Billeteras digitales. */
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

        /**
         * Horario disponible del venue.
         *
         * @param scheduleType DAY(1), NIGHT(2).
         * @param status       INACTIVE(0), ACTIVE(1).
         * @param startTime    hora de inicio.
         * @param endTime      hora de fin.
         * @param price        precio por reserva.
         * @param days         días de la semana.
         */
        public record AvailableVenueItem(
                        @NotNull ScheduleType scheduleType,
                        @NotNull ScheduleStatus status,
                        LocalTime startTime,
                        LocalTime endTime,
                        BigDecimal price,
                        List<AppDayOfWeek> days,
                        // Campos adicionales por horario (opcional)
                        Integer minHours,
                        Integer maxHours,
                        BigDecimal extraHourPrice,
                        Integer minAdvanceDays,
                        String cancellationPolicy) {
        }

        /**
         * Cuenta bancaria asociada al venue.
         */
        public record PayBankItem(
                        @NotNull Integer idBank,
                        @NotBlank String nombreTitular,
                        @NotBlank String numeroCuenta,
                        String cci) {
        }

        /**
         * Billetera digital asociada al venue.
         */
        public record PayWalletItem(
                        @NotNull Integer idDigitalWallet,
                        @NotBlank String nombreTitular,
                        @NotBlank String numeroCelular) {
        }

        public record VenueParkingItem(
                        Integer capacity,
                        BigDecimal price) {
        }

        public record VenueBathroomItem(
                        @NotNull @JsonProperty("quantity") Integer amount) {
        }
}
