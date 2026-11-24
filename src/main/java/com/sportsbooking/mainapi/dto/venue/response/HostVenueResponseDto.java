package com.sportsbooking.mainapi.dto.venue.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HostVenueResponseDto {
    private UUID venueId;
    private String name;
    private List<String> eventTypes;
    private double averageRating;
    private long bookingsCount;
    private BigDecimal totalEarnings;
    private String status; // "ACTIVO" o "INACTIVO"
    private boolean isActive;
}
