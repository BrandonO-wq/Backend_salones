package com.sportsbooking.mainapi.jpa.repository;

import com.sportsbooking.mainapi.jpa.entity.VenuePhotoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para {@link VenuePhotoEntity}.
 */
public interface VenuePhotoRepository extends JpaRepository<VenuePhotoEntity, UUID> {

    List<VenuePhotoEntity> findByVenueId(UUID venueId);

    long countByVenueId(UUID venueId);
}
