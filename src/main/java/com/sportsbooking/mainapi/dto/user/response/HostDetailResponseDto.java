package com.sportsbooking.mainapi.dto.user.response;

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
public class HostDetailResponseDto {
    private UUID hostId;
    private String name;
    private String email;
    private String phone;
    private String ubigeo;
    private String ubigeoText;
    private OffsetDateTime createdAt;
    private long venuesCount;
    private long bookingsCount;
    private double averageRating;
    private BigDecimal debtAmount;
    private BigDecimal totalEarnings;
    private OffsetDateTime deadline;
    private long delayDays;
}
