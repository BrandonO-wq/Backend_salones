package com.sportsbooking.mainapi.dto.review.response;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response con el detalle de una reseña.
 *
 * @param id          UUID de la reseña.
 * @param venueId     UUID del venue reseñado.
 * @param userId      UUID del usuario que hizo la reseña.
 * @param points      puntuación de 1 a 5.
 * @param description comentario del usuario (puede ser null).
 * @param createdAt   fecha de creación.
 */
public record ReviewResponse(
        UUID id,
        UUID venueId,
        String reviewerName,
        Integer points,
        String description,
        OffsetDateTime createdAt
) {}
