package com.sportsbooking.mainapi.dto.venue.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueBookingReportDto {
    private UUID id;
    private String name;
    private String ownerName;
    private Long bookingsCount;
    private String address;
    private Double latitude;
    private Double longitude;
    private Long bookingsLastMonthCount;
    private BigDecimal incomeLastPeriod;
    private BigDecimal commissionLastPeriod;
    private String ubigeo;
    private Long reviewsCount;
    private Double reviewsAverage;
}
