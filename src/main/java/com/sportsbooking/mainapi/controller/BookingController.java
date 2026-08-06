package com.sportsbooking.mainapi.controller;

import com.sportsbooking.mainapi.dto.booking.request.HoldSlotsRequest;
import com.sportsbooking.mainapi.dto.booking.response.BookingResponse;
import com.sportsbooking.mainapi.dto.booking.response.HoldResponse;
import com.sportsbooking.mainapi.dto.booking.response.HostBookingResponse;
import com.sportsbooking.mainapi.dto.booking.response.SlotStatusResponse;
import com.sportsbooking.mainapi.dto.booking.response.SlotValidationResponse;
import com.sportsbooking.mainapi.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.sportsbooking.mainapi.dto.booking.response.ClientBookingResponse;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking", description = "Endpoints para la gestión de reservas")
public class BookingController {

    private final BookingService bookingService;

    @GetMapping("/slots")
    @Operation(summary = "Obtener slots disponibles por fecha")
    public ResponseEntity<SlotStatusResponse> getAvailableSlots(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(name = "venueId") UUID venueId,
            @RequestParam(name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        SlotStatusResponse response = bookingService.getAvailableSlots(userId, venueId, date);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/hold")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Operation(summary = "Bloquear slots temporalmente (10 min)")
    public ResponseEntity<HoldResponse> holdSlots(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody HoldSlotsRequest request) {
        HoldResponse response = bookingService.holdSlots(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate-slots")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Operation(summary = "Validar disponibilidad de horarios antes de intentar pagarlos")
    public ResponseEntity<SlotValidationResponse> validateSlots(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody HoldSlotsRequest request) {
        SlotValidationResponse response = bookingService.validateSlotAvailability(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Operation(summary = "Confirmar reserva con voucher de pago")
    public ResponseEntity<BookingResponse> createBooking(
            @AuthenticationPrincipal UUID userId,
            @RequestParam("holdId") String holdId,
            @RequestParam("idPaymentMethod") Integer idPaymentMethod,
            @RequestParam(value = "description", required = false) String description,
            @RequestPart(value = "voucher", required = false) MultipartFile voucher) {
        BookingResponse response = bookingService.confirmBooking(userId, holdId, voucher, idPaymentMethod, description);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Operation(summary = "Obtener detalle de reserva")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable("id") UUID id) {
        BookingResponse response = bookingService.getBookingById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/voucher")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Operation(summary = "Obtener voucher de una reserva")
    public ResponseEntity<org.springframework.core.io.Resource> getBookingVoucher(
            @AuthenticationPrincipal UUID userId,
            @PathVariable("id") UUID id) {
        return bookingService.getVoucherResource(userId, id);
    }

    @PostMapping(value = "/{id}/voucher", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Operation(summary = "Subir/actualizar voucher de una reserva")
    public ResponseEntity<BookingResponse> uploadVoucher(
            @AuthenticationPrincipal UUID userId,
            @PathVariable("id") UUID id,
            @RequestPart("voucher") MultipartFile voucher) {
        BookingResponse response = bookingService.uploadVoucher(userId, id, voucher);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/hold/{holdId}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Operation(summary = "Liberar bloqueo manualmente")
    public ResponseEntity<Void> releaseHold(
            @AuthenticationPrincipal UUID userId,
            @PathVariable("holdId") String holdId) {
        bookingService.releaseHold(userId, holdId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/host/{id}/confirm")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Operation(summary = "Host confirma reserva")
    public ResponseEntity<Void> confirmByHost(
            @AuthenticationPrincipal UUID userId,
            @PathVariable("id") UUID id) {
        bookingService.confirmByHost(userId, id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/host/{id}/reject")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Operation(summary = "Host rechaza reserva")
    public ResponseEntity<Void> rejectByHost(
            @AuthenticationPrincipal UUID userId,
            @PathVariable("id") UUID id) {
        bookingService.rejectByHost(userId, id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/client")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Operation(summary = "Obtener reservas del cliente autenticado")
    public ResponseEntity<List<ClientBookingResponse>> getClientBookings(
            @AuthenticationPrincipal UUID userId) {
        List<ClientBookingResponse> responses = bookingService.getClientBookings(userId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/host")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Operation(summary = "Obtener reservas del anfitrión")
    public ResponseEntity<List<HostBookingResponse>> getHostBookings(
            @AuthenticationPrincipal UUID userId) {
        List<HostBookingResponse> responses = bookingService.getHostBookings(userId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/host/summary")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Operation(summary = "Obtener resumen de reservas de todos los anfitriones, incluyendo los dueños de los salones")
    public ResponseEntity<List<com.sportsbooking.mainapi.dto.booking.response.HostBookingSummaryResponse>> getHostBookingSummaries() {
        List<com.sportsbooking.mainapi.dto.booking.response.HostBookingSummaryResponse> responses = bookingService
                .getAllHostBookingSummaries();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/admin/summary")
    @PreAuthorize("hasAnyRole('ROLE_CLIENT', 'ROLE_ADMIN')")
    @Operation(summary = "Obtener resumen de todas las reservas de todos los usuarios (para administrador)")
    public ResponseEntity<List<com.sportsbooking.mainapi.dto.booking.response.HostBookingSummaryResponse>> getAdminBookingSummaries() {
        return ResponseEntity.ok(bookingService.getAllHostBookingSummaries());
    }

    @GetMapping("/host/pending-count")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Operation(summary = "Obtener cantidad de reservas pendientes de confirmación para el anfitrión")
    public ResponseEntity<Map<String, Long>> getPendingCountForHost(
            @AuthenticationPrincipal UUID userId) {
        long count = bookingService.countPendingByHost(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }
}
