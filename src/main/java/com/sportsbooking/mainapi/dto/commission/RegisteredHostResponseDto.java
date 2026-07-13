package com.sportsbooking.mainapi.dto.commission;

import com.sportsbooking.mainapi.jpa.entity.enums.HostDebtStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisteredHostResponseDto {
    private UUID hostId;
    private String hostName;
    private OffsetDateTime registrationDate;
    private String email;
    private String phone;
    private long venuesCount;
    private BigDecimal totalIncomeLastPeriod;
    private BigDecimal commissionLastPeriod;
    private OffsetDateTime deadlineLastPeriod;
    private HostDebtStatus statusLastPeriod;
}
