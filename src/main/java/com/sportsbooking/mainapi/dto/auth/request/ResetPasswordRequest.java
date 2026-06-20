package com.sportsbooking.mainapi.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body para restablecer la contraseña usando un reset token válido.
 *
 * @param resetToken  token opaco obtenido tras verificar el OTP de recovery.
 * @param newPassword nueva contraseña del usuario (mínimo 8 caracteres).
 */
public record ResetPasswordRequest(
        @NotBlank(message = "El campo 'resetToken' es obligatorio")
        String resetToken,

        @NotBlank(message = "El campo 'newPassword' es obligatorio")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        String newPassword
) {}
