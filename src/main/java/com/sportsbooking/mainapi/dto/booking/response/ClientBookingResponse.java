package com.sportsbooking.mainapi.dto.booking.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientBookingResponse {
    private UUID id;
    private UUID venueId;
    private String venueName;
    private String venueImageUrl;
    private String code;
    private LocalDate date;
    private String timeSlot;
    private String address;
    private double latitude;
    private double longitude;
    private String ubigeo;
    private String contactNumber;
    private BigDecimal price;
    private Integer status;
    private String sportName;
    private List<Integer> venueSportIds;
    private Boolean isExpired;
    private Float userReviewPoints;
    private String userReviewComment;
}
