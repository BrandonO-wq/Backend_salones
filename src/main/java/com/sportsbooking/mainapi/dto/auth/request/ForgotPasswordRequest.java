package com.sportsbooking.mainapi.dto.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body para solicitar recuperación de contraseña.
 *
 * @param email dirección de correo del usuario que desea recuperar su contraseña.
 */
public record ForgotPasswordRequest(
        @NotBlank(message = "El campo 'email' es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        String email
) {}
