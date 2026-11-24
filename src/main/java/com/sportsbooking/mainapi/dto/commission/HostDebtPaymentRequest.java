package com.sportsbooking.mainapi.dto.commission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record HostDebtPaymentRequest(
    UUID hostDebtId,

    UUID venueId,

    @NotNull(message = "El monto pagado no puede ser nulo.")
    @Positive(message = "El monto pagado debe ser mayor a cero.")
    BigDecimal paidAmount,

    @NotBlank(message = "El tipo de documento no puede estar vacío.")
    String tipoDoc,

    @NotBlank(message = "El número de documento no puede estar vacío.")
    String nroDocumento
) {}
