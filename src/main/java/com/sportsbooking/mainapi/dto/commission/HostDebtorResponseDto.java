package com.sportsbooking.mainapi.dto.commission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HostDebtorResponseDto {
    private String hostName;
    private String email;
    private long venuesCount;
    private BigDecimal totalIncome;
    private long delayDays;
    private BigDecimal pendingCommission;
}
