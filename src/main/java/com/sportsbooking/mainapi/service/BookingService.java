package com.sportsbooking.mainapi.service;

import com.sportsbooking.mainapi.dto.booking.request.HoldSlotsRequest;
import com.sportsbooking.mainapi.dto.booking.response.BookingResponse;
import com.sportsbooking.mainapi.dto.booking.response.HoldResponse;
import com.sportsbooking.mainapi.dto.booking.response.HostBookingResponse;
import com.sportsbooking.mainapi.dto.booking.response.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BookingService {
        SlotStatusResponse getAvailableSlots(UUID userId, UUID venueId, LocalDate date);

        HoldResponse holdSlots(UUID userId, HoldSlotsRequest request);

        SlotValidationResponse validateSlotAvailability(HoldSlotsRequest request);

        BookingResponse confirmBooking(UUID userId, String holdId, MultipartFile voucher, Integer idPaymentMethod,
                        String description);

        org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> getVoucherResource(UUID userId,
                        UUID bookingId);

        BookingResponse uploadVoucher(UUID userId, UUID bookingId, MultipartFile voucher);

        BookingResponse getBookingById(UUID bookingId);

        void releaseHold(UUID userId, String holdId);

        void confirmByHost(UUID hostId, UUID bookingId);

        void confirmBySystem(UUID bookingId);

        void rejectByHost(UUID hostId, UUID bookingId);

        long countPendingByHost(UUID hostId);

        List<HostBookingResponse> getHostBookings(UUID hostId);

        List<com.sportsbooking.mainapi.dto.booking.response.HostBookingSummaryResponse> getHostBookingSummaries(
                        UUID hostId);

        List<com.sportsbooking.mainapi.dto.booking.response.HostBookingSummaryResponse> getAllHostBookingSummaries();

        List<ClientBookingResponse> getClientBookings(UUID clientId);
}
