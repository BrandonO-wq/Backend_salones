package com.sportsbooking.mainapi.dto.review.response;

/**
 * Response con el resumen de reviews de un venue.
 *
 * @param avgRating   promedio de puntuación (0 si no hay reviews).
 * @param reviewCount cantidad total de reviews.
 */
public record VenueRatingSummary(
        double avgRating,
        long reviewCount
) {}
