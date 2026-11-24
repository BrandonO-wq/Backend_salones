package com.sportsbooking.mainapi.dto.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Datos requeridos para iniciar sesión.
 *
 * <p>Ejemplo de uso:
 * <pre>{@code
 * {
 *   "email":    "juan.perez@gmail.com",
 *   "password": "Password1"
 * }
 * }</pre>
 */
public record LoginRequest(

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {}
