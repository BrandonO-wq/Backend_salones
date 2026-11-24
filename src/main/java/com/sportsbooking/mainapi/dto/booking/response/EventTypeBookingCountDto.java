package com.sportsbooking.mainapi.dto.booking.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventTypeBookingCountDto {
    private Integer eventTypeId;
    private String eventTypeName;
    private String description;
    private String icon;
    private Integer iconType;
    private Long bookingCount;
    private Double percentage;
}
