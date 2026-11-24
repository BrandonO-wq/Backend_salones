package com.sportsbooking.mainapi.dto.booking.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotStatusResponse {
    private List<SlotAvailability> daySlots;
    private List<SlotAvailability> nightSlots;
}
