package com.sportsbooking.mainapi.service;

import com.sportsbooking.mainapi.dto.common.MessageResponse;
import com.sportsbooking.mainapi.dto.review.request.CreateReviewRequest;
import com.sportsbooking.mainapi.dto.review.response.ReviewResponse;
import com.sportsbooking.mainapi.dto.review.response.VenueRatingSummary;

import com.sportsbooking.mainapi.dto.common.PaginatedResponse;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

/**
 * Contrato de negocio para reviews de venues.
 */
public interface ReviewService {

    /**
     * Crea una reseña para un venue. Un usuario solo puede dejar una por venue.
     *
     * @param userId  ID del usuario autenticado.
     * @param venueId ID del venue a reseñar.
     * @param request datos de la reseña.
     * @return detalle de la reseña creada.
     */
    ReviewResponse createReview(UUID userId, UUID venueId, CreateReviewRequest request);

    /**
     * Actualiza la reseña existente de un usuario sobre un venue.
     *
     * @param userId  ID del usuario autenticado.
     * @param venueId ID del venue.
     * @param request datos actualizados de la reseña.
     * @return detalle de la reseña actualizada.
     */
    ReviewResponse updateReview(UUID userId, UUID venueId, CreateReviewRequest request);

    /**
     * Elimina la reseña de un usuario sobre un venue.
     *
     * @param userId  ID del usuario autenticado.
     * @param venueId ID del venue.
     * @return mensaje de confirmación.
     */
    MessageResponse deleteReview(UUID userId, UUID venueId, UUID bookingId);

    /**
     * Lista todas las reseñas de un venue (paginado).
     *
     * @param venueId ID del venue.
     * @param pageable Configuración de paginación.
     * @return respuesta paginada de reseñas.
     */
    PaginatedResponse<ReviewResponse> getReviewsByVenue(UUID venueId, Pageable pageable);

    /**
     * Obtiene el resumen de rating de un venue (promedio y cantidad).
     *
     * @param venueId ID del venue.
     * @return resumen con avgRating y reviewCount.
     */
    VenueRatingSummary getRatingSummary(UUID venueId);
}
