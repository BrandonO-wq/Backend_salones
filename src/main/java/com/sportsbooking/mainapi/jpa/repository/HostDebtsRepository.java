package com.sportsbooking.mainapi.jpa.repository;

import com.sportsbooking.mainapi.jpa.entity.HostDebts;
import com.sportsbooking.mainapi.jpa.entity.enums.HostDebtStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface HostDebtsRepository extends JpaRepository<HostDebts, UUID> {
    boolean existsByCycleStart(LocalDate cycleStart);
    List<HostDebts> findByStatusNot(HostDebtStatus status);
    List<HostDebts> findByHostUserIdAndStatusNot(UUID hostUserId, HostDebtStatus status);
    java.util.Optional<HostDebts> findByVenueIdAndCycleStart(UUID venueId, LocalDate cycleStart);
    java.util.Optional<HostDebts> findByBookingId(UUID bookingId);
    List<HostDebts> findListByVenueIdAndCycleStart(UUID venueId, LocalDate cycleStart);
    List<HostDebts> findByVenueIdAndCycleEndBeforeAndStatusNot(UUID venueId, LocalDate cycleEnd, HostDebtStatus status);
}
