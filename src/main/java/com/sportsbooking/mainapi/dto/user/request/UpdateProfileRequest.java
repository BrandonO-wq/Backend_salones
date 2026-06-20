package com.sportsbooking.mainapi.dto.user.request;

import jakarta.validation.constraints.Size;

/**
 * Request body para {@code PATCH /users/profile}.
 *
 * <p>
 * Actualización parcial: solo se modifican los campos que se envían (no nulos
 * ni vacíos).
 * Si un campo es {@code null} o no se incluye en el JSON, se ignora y mantiene
 * su valor actual.
 *
 * @param name   Nombre completo del usuario (opcional).
 * @param phone  Teléfono / celular del usuario (opcional).
 * @param ubigeo Código ubigeo del usuario (opcional).
 */
public record UpdateProfileRequest(

                @Size(min = 1, message = "El nombre no puede estar vacío si se envía") String name,

                @Size(min = 1, message = "El teléfono no puede estar vacío si se envía") String phone,
                String ubigeo) {
}
