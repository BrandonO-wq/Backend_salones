package com.sportsbooking.mainapi.dto.commission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO para el reporte de deudas de salones/venues de cara al administrador.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueDebtReportResponseDto {
    private UUID venueId;
    private String venueName;
    private String ownerName;
    private String ownerEmail;
    private long delayDays;
    private String address;
    private double latitude;
    private double longitude;
    private String ubigeo;
    private double rating;
    private String debtStatus; // "PENDING", "PAID", "OVERDUE", "LEGACY_PENDING", "NONE"
    private double commissionAmount;
    private double totalIncome;
    private OffsetDateTime deadline;
}
