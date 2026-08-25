package com.sportsbooking.mainapi.dto.booking.response;

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
public class HostBookingResponse {
    private UUID id;
    private String venueName;
    private String code;
    private LocalDate date;
    private String timeSlot;
    private String clientName;
    private String clientPhone;
    private BigDecimal amount;
    private BigDecimal commissionRate;
    private BigDecimal commissionAmount;
    private BigDecimal netAmount;
    private Integer status;
    private Boolean paid;
    private Boolean isExpired;
    private String voucherUrl;
    private String message;
    private OffsetDateTime confirmedAt;
}
