package com.sportsbooking.mainapi.jpa.repository;

import com.sportsbooking.mainapi.jpa.entity.VenueReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para {@link VenueReviewEntity}.
 */
public interface VenueReviewRepository extends JpaRepository<VenueReviewEntity, UUID> {

    Page<VenueReviewEntity> findByVenueIdOrderByCreatedAtDesc(UUID venueId, Pageable pageable);

    Optional<VenueReviewEntity> findByBookingIdAndUserId(UUID bookingId, UUID userId);

    boolean existsByBookingIdAndUserId(UUID bookingId, UUID userId);

    long countByVenueId(UUID venueId);

    @Query("SELECT COALESCE(AVG(r.points), 0.0) FROM VenueReviewEntity r WHERE r.venueId = :venueId")
    double avgPointsByVenueId(@Param("venueId") UUID venueId);

    @Query("SELECT COALESCE(AVG(r.points), 0.0) FROM VenueReviewEntity r JOIN VenueEntity v ON r.venueId = v.id WHERE v.hostUserId = :hostUserId")
    double avgPointsByHostUserId(@Param("hostUserId") UUID hostUserId);
}
