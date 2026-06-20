package com.sportsbooking.mainapi.dto.booking.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record HostBookingSummaryResponse(
                UUID bookingId,
                UUID venueId,
                String venueName,
                LocalDate bookingDate,
                OffsetDateTime createdAt,
                BigDecimal totalAmount,
                List<String> eventTypes,
                double avgRating,
                long reviewCount,
                List<String> schedules) {
}
