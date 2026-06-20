package com.sportsbooking.mainapi.dto.booking.response;

import java.util.List;

public record SlotValidationResponse(
        List<SlotItemResult> available,
        List<SlotItemResult> unavailable
) {
}
