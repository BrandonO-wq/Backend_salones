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
}
