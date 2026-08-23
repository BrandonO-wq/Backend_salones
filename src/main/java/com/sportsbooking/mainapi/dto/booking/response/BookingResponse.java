package com.sportsbooking.mainapi.dto.booking.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private UUID id;
    private UUID venueId;
    private UUID clientUserId;
    private LocalDate bookingDate;
    private Integer totalHours;
    private BigDecimal totalAmount;
    private Integer status;
    private Integer idPaymentMethod;
    private String bookingCode;
    private List<Integer> sportsId;
    private String sportName;
}
