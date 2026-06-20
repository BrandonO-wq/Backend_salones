package com.sportsbooking.mainapi.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request body para verificar el código OTP de registro.
 *
 * <p>El {@code userId} fue retornado por {@code POST /auth/register}.
 * El {@code otpCode} fue enviado al email del usuario.
 *
 * @param userId  ID del usuario que se está verificando.
 * @param otpCode código OTP de 4 dígitos recibido por email.
 */
public record VerifyOtpRequest(
        @NotNull(message = "El campo 'userId' es obligatorio")
        UUID userId,

        @NotBlank(message = "El campo 'otpCode' es obligatorio")
        String otpCode
) {
}
