package com.sportsbooking.mainapi.jpa.repository;

import com.sportsbooking.mainapi.jpa.entity.VenueParkingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VenueParkingRepository extends JpaRepository<VenueParkingEntity, UUID> {
    List<VenueParkingEntity> findByVenueId(UUID venueId);
    void deleteByVenueId(UUID venueId);
}
