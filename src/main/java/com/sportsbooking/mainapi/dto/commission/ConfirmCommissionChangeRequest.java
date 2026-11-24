package com.sportsbooking.mainapi.dto.commission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * DTO para la solicitud de confirmación del cambio de comisión.
 *
 * @param otpCode  código de verificación OTP enviado por correo electrónico.
 * @param newValue nuevo valor de comisión (debe ser mayor o igual a cero).
 */
public record ConfirmCommissionChangeRequest(
    @NotBlank(message = "El código OTP no puede estar vacío.")
    String otpCode,

    @NotNull(message = "El nuevo valor de comisión no puede ser nulo.")
    @PositiveOrZero(message = "El valor de comisión debe ser mayor o igual a cero.")
    Double newValue
) {}
