package com.sportsbooking.mainapi.dto.commission;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para la solicitud de confirmación del cambio de configuración del ciclo de comisiones.
 */
public record ConfirmCycleConfigChangeRequest(
    @NotBlank(message = "El código OTP no puede estar vacío.")
    String otpCode,

    @NotNull(message = "La duración del ciclo no puede ser nula.")
    @Min(value = 1, message = "La duración del ciclo debe ser de al menos 1 día.")
    @Max(value = 365, message = "La duración del ciclo no puede superar los 365 días.")
    Integer cycleDays,

    @NotNull(message = "Los días límite de pago no pueden ser nulos.")
    @Min(value = 0, message = "Los días límite de pago deben ser mayores o iguales a cero.")
    @Max(value = 365, message = "Los días límite de pago no pueden superar los 365 días.")
    Integer deadlineOffsetDays,

    @NotNull(message = "El día de inicio de ciclo no puede ser nulo.")
    @Min(value = 1, message = "El día de inicio debe estar entre 1 (Lunes) y 7 (Domingo).")
    @Max(value = 7, message = "El día de inicio debe estar entre 1 (Lunes) y 7 (Domingo).")
    Integer startDayOfWeek,

    @NotNull(message = "La hora de inicio no puede ser nula.")
    @Min(value = 0, message = "La hora de inicio debe estar entre 0 y 23.")
    @Max(value = 23, message = "La hora de inicio debe estar entre 0 y 23.")
    Integer startHour
) {}
