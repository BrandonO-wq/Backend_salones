package com.sportsbooking.mainapi.dto.auth.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Datos requeridos para iniciar sesión con Google.
 *
 * <p>Ejemplo de uso:
 * <pre>{@code
 * {
 *   "idToken": "eyJhbGciOiJSUzI1Ni..."
 * }
 * }</pre>
 */
public record GoogleLoginRequest(

        @NotBlank(message = "El idToken de Google es obligatorio")
        String idToken
) {}
