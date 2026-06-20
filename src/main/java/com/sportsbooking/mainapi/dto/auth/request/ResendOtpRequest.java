package com.sportsbooking.mainapi.dto.auth.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request body para reenviar el código OTP de registro.
 *
 * <p>Solo puede usarse si el usuario está en estado INACTIVO no verificado y el cooldown ha expirado.
 *
 * @param userId ID del usuario que solicita el reenvío de OTP.
 */
public record ResendOtpRequest(
        @NotNull(message = "El campo 'userId' es obligatorio")
        UUID userId
) {
}
