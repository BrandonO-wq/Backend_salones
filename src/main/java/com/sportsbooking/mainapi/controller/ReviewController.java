package com.sportsbooking.mainapi.controller;

import com.sportsbooking.mainapi.dto.common.ErrorResponse;
import com.sportsbooking.mainapi.dto.common.MessageResponse;
import com.sportsbooking.mainapi.dto.review.request.CreateReviewRequest;
import com.sportsbooking.mainapi.dto.review.response.ReviewResponse;
import com.sportsbooking.mainapi.dto.review.response.VenueRatingSummary;
import com.sportsbooking.mainapi.service.ReviewService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.sportsbooking.mainapi.dto.common.PaginatedResponse;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para reviews de venues.
 *
 * <p>
 * Prefijo de ruta: {@code /venues/{venueId}/reviews}.
 *
 * <p>
 * Endpoints públicos: {@code GET} (listado y resumen).
 * <br>
 * Endpoints protegidos (JWT): {@code POST}, {@code PUT}, {@code DELETE}.
 */
@Tag(name = "Venue Reviews", description = "Gestión de reseñas de venues")
@RestController
@RequestMapping("/venues/{venueId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

        private final ReviewService reviewService;

        // ─────────────────────────────────────────────
        // POST /venues/{venueId}/reviews (JWT)
        // ─────────────────────────────────────────────

        /**
         * Crea una reseña para un venue. Un usuario solo puede dejar una por venue.
         */
        @Operation(summary = "Crear reseña", description = "Crea una reseña de 1-5 estrellas para un venue. Solo una por usuario.", security = @SecurityRequirement(name = "bearerAuth"), responses = {
                        @ApiResponse(responseCode = "201", description = "Reseña creada", content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "409", description = "Ya existe una reseña de este usuario", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        })
        @PostMapping
        public ResponseEntity<ReviewResponse> createReview(
                        @PathVariable("venueId") UUID venueId,
                        @Valid @RequestBody CreateReviewRequest request,
                        @AuthenticationPrincipal UUID userId) {
                ReviewResponse response = reviewService.createReview(userId, venueId, request);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        // ─────────────────────────────────────────────
        // PUT /venues/{venueId}/reviews (JWT)
        // ─────────────────────────────────────────────

        /**
         * Actualiza la reseña del usuario autenticado sobre un venue.
         */
        @Operation(summary = "Actualizar reseña", description = "Actualiza la reseña existente del usuario sobre el venue.", security = @SecurityRequirement(name = "bearerAuth"), responses = {
                        @ApiResponse(responseCode = "200", description = "Reseña actualizada", content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Reseña no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        })
        @PutMapping
        public ResponseEntity<ReviewResponse> updateReview(
                        @PathVariable("venueId") UUID venueId,
                        @Valid @RequestBody CreateReviewRequest request,
                        @AuthenticationPrincipal UUID userId) {
                ReviewResponse response = reviewService.updateReview(userId, venueId, request);
                return ResponseEntity.ok(response);
        }

        // ─────────────────────────────────────────────
        // DELETE /venues/{venueId}/reviews (JWT)
        // ─────────────────────────────────────────────

        /**
         * Elimina la reseña del usuario autenticado sobre un venue.
         */
        @Operation(summary = "Eliminar reseña", description = "Elimina la reseña del usuario autenticado sobre el venue.", security = @SecurityRequirement(name = "bearerAuth"), responses = {
                        @ApiResponse(responseCode = "200", description = "Reseña eliminada", content = @Content(schema = @Schema(implementation = MessageResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Reseña no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        })
        @DeleteMapping("/{bookingId}")
        public ResponseEntity<MessageResponse> deleteReview(
                        @PathVariable("venueId") UUID venueId,
                        @PathVariable("bookingId") UUID bookingId,
                        @AuthenticationPrincipal UUID userId) {
                MessageResponse response = reviewService.deleteReview(userId, venueId, bookingId);
                return ResponseEntity.ok(response);
        }

        // ─────────────────────────────────────────────
        // GET /venues/{venueId}/reviews (público)
        // ─────────────────────────────────────────────

        /**
         * Lista todas las reseñas de un venue (ordenadas por fecha desc).
         */
        @Operation(summary = "Listar reseñas", description = "Retorna todas las reseñas de un venue ordenadas de más reciente a más antigua.", responses = {
                        @ApiResponse(responseCode = "200", description = "Lista de reseñas", content = @Content(schema = @Schema(implementation = ReviewResponse.class)))
        })
        @GetMapping
        public ResponseEntity<PaginatedResponse<ReviewResponse>> getReviews(
                        @PathVariable("venueId") UUID venueId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
                PaginatedResponse<ReviewResponse> reviews = reviewService.getReviewsByVenue(venueId,
                                PageRequest.of(page, size));
                return ResponseEntity.ok(reviews);
        }

        // ─────────────────────────────────────────────
        // GET /venues/{venueId}/reviews/summary (público)
        // ─────────────────────────────────────────────

        /**
         * Resumen de rating de un venue (promedio y cantidad de reviews).
         */
        @Operation(summary = "Resumen de rating", description = "Retorna el promedio de estrellas y cantidad total de reseñas de un venue.", responses = {
                        @ApiResponse(responseCode = "200", description = "Resumen de rating", content = @Content(schema = @Schema(implementation = VenueRatingSummary.class)))
        })
        @GetMapping("/summary")
        public ResponseEntity<VenueRatingSummary> getRatingSummary(@PathVariable("venueId") UUID venueId) {
                VenueRatingSummary summary = reviewService.getRatingSummary(venueId);
                return ResponseEntity.ok(summary);
        }
}
