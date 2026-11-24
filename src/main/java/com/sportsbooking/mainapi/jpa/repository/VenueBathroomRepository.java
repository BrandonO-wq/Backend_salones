package com.sportsbooking.mainapi.jpa.repository;

import com.sportsbooking.mainapi.jpa.entity.VenueBathroomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VenueBathroomRepository extends JpaRepository<VenueBathroomEntity, UUID> {
    List<VenueBathroomEntity> findByVenueId(UUID venueId);
    void deleteByVenueId(UUID venueId);
}
