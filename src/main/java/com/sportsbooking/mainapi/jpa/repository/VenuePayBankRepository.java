package com.sportsbooking.mainapi.jpa.repository;

import com.sportsbooking.mainapi.jpa.entity.VenuePayBankEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para {@link VenuePayBankEntity}.
 */
public interface VenuePayBankRepository extends JpaRepository<VenuePayBankEntity, UUID> {

    List<VenuePayBankEntity> findByVenueId(UUID venueId);

    void deleteByVenueId(UUID venueId);
}
