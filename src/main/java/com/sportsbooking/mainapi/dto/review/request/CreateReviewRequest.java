package com.sportsbooking.mainapi.dto.review.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request para crear o actualizar una reseña de venue.
 *
 * @param points      puntuación de 1 a 5 estrellas (obligatorio).
 * @param bookingId   ID de la reserva asociada (obligatorio).
 * @param description comentario opcional (máx 500 caracteres).
 */
public record CreateReviewRequest(

        @NotNull(message = "La puntuación es obligatoria")
        @Min(value = 1, message = "La puntuación mínima es 1")
        @Max(value = 5, message = "La puntuación máxima es 5")
        Integer points,

        @NotNull(message = "El ID de la reserva es requerido")
        java.util.UUID bookingId,

        @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
        String description
) {}
