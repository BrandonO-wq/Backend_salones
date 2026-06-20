package com.sportsbooking.mainapi.dto.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos requeridos para registrar un nuevo usuario.
 *
 * <p>Restricciones de validación:
 * <ul>
 * <li>{@code name} – obligatorio, no vacío.</li>
 * <li>{@code email} – obligatorio, formato válido de email.</li>
 * <li>{@code phone} – opcional.</li>
 * <li>{@code password} – mínimo 6 caracteres, debe tener al menos una letra y
 * un número.</li>
 * </ul>
 *
 * <p>Ejemplo de uso:
 * <pre>{@code
 * {
 * "name": "Juan Perez",
 * "email": "juan.perez@gmail.com",
 * "phone": "987654321",
 * "password": "Password1"
 * }
 * }</pre>
 */
public record RegisterRequest(

                @NotBlank(message = "El nombre es obligatorio") String name,

                @NotBlank(message = "El email es obligatorio") @Email(message = "El email no tiene un formato válido") String email,

                String phone,

                @NotBlank(message = "La contraseña es obligatoria") @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres") @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "La contraseña debe contener al menos una letra y un número") String password,

                // Código ubigeo (ej: "020601"). Opcional.
                String ubigeo) {
}
