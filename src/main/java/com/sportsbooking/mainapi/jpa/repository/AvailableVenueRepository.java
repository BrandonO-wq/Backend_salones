package com.sportsbooking.mainapi.jpa.repository;

import com.sportsbooking.mainapi.jpa.entity.AvailableVenueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para {@link AvailableVenueEntity}.
 */
public interface AvailableVenueRepository extends JpaRepository<AvailableVenueEntity, UUID> {

    List<AvailableVenueEntity> findByVenueId(UUID venueId);
    List<AvailableVenueEntity> findByVenueIdAndStatus(UUID venueId, int status);

    void deleteByVenueId(UUID venueId);
}
