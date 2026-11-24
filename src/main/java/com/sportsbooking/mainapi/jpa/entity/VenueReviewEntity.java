package com.sportsbooking.mainapi.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad que representa una reseña de un usuario sobre un venue.
 *
 * <p>Mapeada a la tabla {@code venue_reviews}.
 * {@code points} va de 1 a 5 estrellas (constraint CHECK en BD).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "venue_reviews")
public class VenueReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "venue_id", nullable = false)
    private UUID venueId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "description")
    private String description;

    /** Puntuación de 1 a 5 estrellas. */
    @Column(name = "points", nullable = false)
    private Integer points;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
