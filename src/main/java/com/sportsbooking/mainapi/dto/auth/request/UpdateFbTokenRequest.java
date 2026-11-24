package com.sportsbooking.mainapi.dto.auth.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body para actualizar el token de Firebase del usuario autenticado.
 *
 * @param fbToken token de Firebase (FCM) del dispositivo del usuario.
 */
public record UpdateFbTokenRequest(
        @NotBlank(message = "El campo 'fbToken' es obligatorio")
        String fbToken
) {
}
