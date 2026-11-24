package com.sportsbooking.mainapi.dto.auth.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Datos requeridos para renovar el par de tokens mediante el refresh token.
 *
 * <p>Ejemplo de uso:
 * <pre>{@code
 * {
 *   "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
 * }
 * }</pre>
 */
public record RefreshTokenRequest(

        @NotBlank(message = "El refresh token es obligatorio")
        String refreshToken
) {}
