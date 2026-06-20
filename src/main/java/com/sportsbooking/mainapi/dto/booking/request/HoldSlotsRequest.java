package com.sportsbooking.mainapi.dto.booking.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldSlotsRequest {

    @NotNull(message = "Venue ID is required")
    private UUID venueId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotEmpty(message = "At least one slot must be selected")
    @Valid
    private List<SlotItem> slots;
}
