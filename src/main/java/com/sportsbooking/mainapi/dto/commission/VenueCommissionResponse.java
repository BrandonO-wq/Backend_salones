package com.sportsbooking.mainapi.dto.commission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueCommissionResponse {

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

    /** Fecha límite de pago (deadline). */
    private OffsetDateTime deadline;

    /** Porcentaje de comisión aplicado. */
    private BigDecimal commissionRate;
}
