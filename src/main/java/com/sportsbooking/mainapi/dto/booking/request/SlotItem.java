package com.sportsbooking.mainapi.dto.booking.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotItem {
    @NotNull(message = "Schedule type is required")
    private Integer scheduleType;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;
}
