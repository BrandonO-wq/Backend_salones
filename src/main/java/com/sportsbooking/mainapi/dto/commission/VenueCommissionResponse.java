package com.sportsbooking.mainapi.dto.commission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueCommissionResponse {

    /** ID de la deuda en host_debts. */
    private UUID hostDebtId;

    /** Nombre del salón. */
    private String venueName;

    /** Fecha de inicio del ciclo (Lunes). */
    private LocalDate cycleStart;

    /** Fecha de fin del ciclo (Domingo). */
    private LocalDate cycleEnd;

    /** Ingresos totales del salón en el ciclo. */
    private BigDecimal totalIncome;

    /** Comisión a pagar. */
    private BigDecimal commissionAmount;

    /** Monto ya pagado/adelantado en este ciclo. */
    private BigDecimal paidAmount;

    /** Monto pendiente de comisión por pagar. */
    private BigDecimal pendingAmount;

    /** Fecha límite de pago (deadline). */
    private OffsetDateTime deadline;

    /** Porcentaje de comisión aplicado. */
    private BigDecimal commissionRate;

    /** Estado de la deuda (1=PENDING, 2=PAID, 3=OVERDUE). */
    private Integer status;
}
