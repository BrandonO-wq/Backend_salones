package com.sportsbooking.mainapi.dto.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body para verificar el OTP de recuperación de contraseña.
 *
 * <p>Usa {@code email} en vez de {@code userId} para evitar filtrar
 * la existencia de cuentas en el flujo de recovery.
 *
 * @param email   email del usuario que solicitó la recuperación.
 * @param otpCode código OTP de 4 dígitos recibido por email.
 */
public record VerifyRecoveryOtpRequest(
        @NotBlank(message = "El campo 'email' es obligatorio")
        @Email(message = "El campo 'email' debe ser un email válido")
        String email,

        @NotBlank(message = "El campo 'otpCode' es obligatorio")
        String otpCode
) {}
