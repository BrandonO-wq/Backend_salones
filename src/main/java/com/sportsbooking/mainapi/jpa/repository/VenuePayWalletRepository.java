package com.sportsbooking.mainapi.jpa.repository;

import com.sportsbooking.mainapi.jpa.entity.VenuePayWalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para {@link VenuePayWalletEntity}.
 */
public interface VenuePayWalletRepository extends JpaRepository<VenuePayWalletEntity, UUID> {

    List<VenuePayWalletEntity> findByVenueId(UUID venueId);

    void deleteByVenueId(UUID venueId);
}
