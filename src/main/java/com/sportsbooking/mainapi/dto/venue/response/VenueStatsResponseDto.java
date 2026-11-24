package com.sportsbooking.mainapi.dto.venue.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VenueStatsResponseDto {
    private Long totalSalones;
    private Long totalPropietarios;
}
