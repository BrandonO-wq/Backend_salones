package com.sportsbooking.mainapi.dto.booking.response;

public record SlotItemResult(
        int scheduleType,
        String startTime,
        String reason
) {
}
