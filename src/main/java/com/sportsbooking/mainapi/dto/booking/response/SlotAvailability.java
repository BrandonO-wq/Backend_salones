package com.sportsbooking.mainapi.dto.booking.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotAvailability {
    private String id;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal price;
    private String status; // AVAILABLE, BOOKED, HELD
}
