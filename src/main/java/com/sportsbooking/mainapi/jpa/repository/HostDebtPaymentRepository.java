package com.sportsbooking.mainapi.jpa.repository;

import com.sportsbooking.mainapi.dto.commission.HostDebtPaymentResponse;
import com.sportsbooking.mainapi.jpa.entity.HostDebtPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HostDebtPaymentRepository extends JpaRepository<HostDebtPayment, UUID> {
    @Query("SELECT new com.sportsbooking.mainapi.dto.commission.HostDebtPaymentResponse(" +
           "  p.id, p.createdAt, v.name, p.tipoDoc, p.nroDocumento, p.paidAmount, p.status," +
           "  SUM(d.totalIncome)" +
           ") " +
           "FROM HostDebtPayment p " +
           "JOIN HostDebts d ON d.idCommission = p.id " +
           "JOIN VenueEntity v ON d.venueId = v.id " +
           "WHERE (:hostUserId IS NULL OR d.hostUserId = :hostUserId) " +
           "GROUP BY p.id, p.createdAt, v.name, p.tipoDoc, p.nroDocumento, p.paidAmount, p.status")
    List<HostDebtPaymentResponse> findPaymentsByHostUserId(@Param("hostUserId") UUID hostUserId);
}
