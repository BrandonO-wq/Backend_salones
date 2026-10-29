package com.sportsbooking.mainapi.jpa.entity;

import com.sportsbooking.mainapi.jpa.entity.converter.HostDebtStatusConverter;
import com.sportsbooking.mainapi.jpa.entity.enums.HostDebtStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "host_debts")
public class HostDebts {

    /**
     * Identificador único de la deuda.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * ID del usuario HOST propietario del venue.
     */
    @Column(name = "host_user_id", nullable = false)
    private UUID hostUserId;

    /**
     * ID del venue al que pertenece la deuda.
     */
    @Column(name = "venue_id", nullable = false)
    private UUID venueId;

    /**
     * ID de la reserva asociada (1 reserva = 1 deuda).
     */
    @Column(name = "booking_id")
    private UUID bookingId;

    /**
     * Fecha de inicio del ciclo de facturación.
     */
    @Column(name = "cycle_start", nullable = false)
    private LocalDate cycleStart;

    /**
     * Fecha de fin del ciclo de facturación.
     */
    @Column(name = "cycle_end", nullable = false)
    private LocalDate cycleEnd;

    /**
     * Fecha y hora límite para realizar el pago de la comisión.
     */
    @Column(name = "deadline", nullable = false)
    private OffsetDateTime deadline;

    @Column(name = "total_income", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalIncome;

    @Column(name = "commission_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Convert(converter = HostDebtStatusConverter.class)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private HostDebtStatus status = HostDebtStatus.PENDING;

    @Column(name = "id_commission")
    private UUID idCommission;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
