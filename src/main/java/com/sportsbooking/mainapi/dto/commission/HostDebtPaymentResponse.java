package com.sportsbooking.mainapi.dto.commission;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record HostDebtPaymentResponse(
    UUID id,
    OffsetDateTime fechaPago,
    String nombreSalon,
    String tipoDoc,
    String nroDocumento,
    BigDecimal monto,
    Integer estado,
    BigDecimal totalIngresos
) {}
