package com.sportsbooking.mainapi.service.impl;

import com.sportsbooking.mainapi.common.exception.ResourceNotFoundException;
import com.sportsbooking.mainapi.dto.common.MessageResponse;
import com.sportsbooking.mainapi.dto.review.request.CreateReviewRequest;
import com.sportsbooking.mainapi.dto.review.response.ReviewResponse;
import com.sportsbooking.mainapi.dto.review.response.VenueRatingSummary;
import com.sportsbooking.mainapi.jpa.entity.VenueReviewEntity;
import com.sportsbooking.mainapi.jpa.repository.VenueRepository;
import com.sportsbooking.mainapi.jpa.repository.VenueReviewRepository;
import com.sportsbooking.mainapi.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.sportsbooking.mainapi.dto.common.PaginatedResponse;
import com.sportsbooking.mainapi.jpa.repository.UserRepository;
import com.sportsbooking.mainapi.jpa.entity.UserEntity;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de reviews de venues.
 *
 * <p>Valida existencia del venue y restricción de una sola reseña por usuario.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final VenueReviewRepository reviewRepository;
    private final VenueRepository venueRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ReviewResponse createReview(UUID userId, UUID venueId, CreateReviewRequest request) {
        validateVenueExists(venueId);

        if (reviewRepository.existsByBookingIdAndUserId(request.bookingId(), userId)) {
            throw new IllegalStateException(
                    "Ya has dejado una reseña para esta reserva. Usa PUT para actualizarla.");
        }

        VenueReviewEntity review = VenueReviewEntity.builder()
                .venueId(venueId)
                .userId(userId)
                .bookingId(request.bookingId())
                .points(request.points())
                .description(request.description())
                .build();

        VenueReviewEntity saved = reviewRepository.save(review);
        log.info("[Review] Creada por user {} en venue {} — {} estrellas", userId, venueId, request.points());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(UUID userId, UUID venueId, CreateReviewRequest request) {
        VenueReviewEntity review = reviewRepository.findByBookingIdAndUserId(request.bookingId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Review", "bookingId=" + request.bookingId() + ", userId=" + userId));

        review.setPoints(request.points());
        review.setDescription(request.description());

        VenueReviewEntity updated = reviewRepository.save(review);
        log.info("[Review] Actualizada por user {} en venue {} — {} estrellas", userId, venueId, request.points());

        return toResponse(updated);
    }

    @Override
    @Transactional
    public MessageResponse deleteReview(UUID userId, UUID venueId, UUID bookingId) {
        VenueReviewEntity review = reviewRepository.findByBookingIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Review", "bookingId=" + bookingId + ", userId=" + userId));

        reviewRepository.delete(review);
        log.info("[Review] Eliminada por user {} en venue {}", userId, venueId);

        return new MessageResponse("Reseña eliminada correctamente");
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ReviewResponse> getReviewsByVenue(UUID venueId, Pageable pageable) {
        validateVenueExists(venueId);
        Page<VenueReviewEntity> page = reviewRepository.findByVenueIdOrderByCreatedAtDesc(venueId, pageable);
        
        List<ReviewResponse> content = page.getContent()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PaginatedResponse.<ReviewResponse>builder()
                .content(content)
                .number(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public VenueRatingSummary getRatingSummary(UUID venueId) {
        validateVenueExists(venueId);
        double avg = reviewRepository.avgPointsByVenueId(venueId);
        long count = reviewRepository.countByVenueId(venueId);
        return new VenueRatingSummary(
                Math.round(avg * 10.0) / 10.0,
                count
        );
    }

    // ─────────────────────────────────────────────
    // Métodos privados
    // ─────────────────────────────────────────────

    /**
     * Valida que el venue exista y esté activo.
     *
     * @throws ResourceNotFoundException si no existe.
     */
    private void validateVenueExists(UUID venueId) {
        if (!venueRepository.existsById(venueId)) {
            throw new ResourceNotFoundException("Venue", venueId);
        }
    }

    /**
     * Mapea la entidad a DTO de response, ofuscando el nombre del usuario.
     */
    private ReviewResponse toResponse(VenueReviewEntity entity) {
        String fullName = userRepository.findById(entity.getUserId())
                .map(UserEntity::getName)
                .orElse("Usuario Anónimo");

        return new ReviewResponse(
                entity.getId(),
                entity.getVenueId(),
                formatPrivacyName(fullName),
                entity.getPoints(),
                entity.getDescription(),
                entity.getCreatedAt()
        );
    }

    /**
     * Aplica la regla de privacidad: "Aaron Cisneros Vargas" -> "Aaron C."
     */
    private String formatPrivacyName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Usuario Anónimo";
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0];
        }
        return parts[0] + " " + parts[1].charAt(0) + ".";
    }
}
