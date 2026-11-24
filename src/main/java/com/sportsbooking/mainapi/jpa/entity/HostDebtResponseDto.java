package com.sportsbooking.mainapi.jpa.entity;

import com.sportsbooking.mainapi.jpa.entity.enums.HostDebtStatus;
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
public class HostDebtResponseDto {
    private UUID id;
    private UUID hostUserId;
    private UUID venueId;
    private UUID bookingId;
    private LocalDate cycleStart;
    private LocalDate cycleEnd;
    private OffsetDateTime deadline;
    private BigDecimal totalIncome;
    private BigDecimal commissionAmount;
    private BigDecimal paidAmount;
    private HostDebtStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
