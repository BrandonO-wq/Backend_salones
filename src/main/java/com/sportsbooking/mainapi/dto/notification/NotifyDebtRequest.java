package com.sportsbooking.mainapi.dto.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NotifyDebtRequest(
        @NotBlank(message = "El título de la notificación es requerido")
        @Size(max = 100, message = "El título no puede exceder los 100 caracteres")
        String title,

        @NotBlank(message = "El cuerpo de la notificación es requerido")
        @Size(max = 500, message = "El cuerpo no puede exceder los 500 caracteres")
        String body
) {
}
