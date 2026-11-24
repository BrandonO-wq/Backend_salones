package com.sportsbooking.mainapi.dto.auth.request;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO para verificar y confirmar la eliminación de una cuenta.
 */
public record VerifyDeleteRequest(
        @NotBlank(message = "El código OTP es obligatorio")
        String otpCode
) {}
