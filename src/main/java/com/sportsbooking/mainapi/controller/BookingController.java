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

import lombok.extern.slf4j.Slf4j;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.sportsbooking.mainapi.dto.booking.response.ClientBookingResponse;

@Slf4j
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
            @RequestParam(value = "sports_id", required = false) String rawSportsId,
            @RequestParam(value = "eventType", required = false) String eventType,
            @RequestParam(value = "guests", required = false) Integer guests,
            @RequestParam(value = "paymentMethodName", required = false) String paymentMethodName,
            @RequestParam(value = "description", required = false) String description,
            @RequestPart(value = "voucher", required = false) MultipartFile voucher,
            jakarta.servlet.http.HttpServletRequest request) {

        List<Integer> extractedSportsId = parseSportsId(rawSportsId, request);
        log.info("[createBooking] userId={}, holdId={}, rawSportsId='{}', extractedSportsId={}",
                userId, holdId, rawSportsId, extractedSportsId);

        BookingResponse response = bookingService.confirmBooking(userId, holdId, voucher, idPaymentMethod, description,
                extractedSportsId);
        return ResponseEntity.ok(response);
    }

    private List<Integer> parseSportsId(String rawSportsId, jakarta.servlet.http.HttpServletRequest request) {
        List<Integer> result = new ArrayList<>();
        List<String> candidates = new ArrayList<>();

        if (rawSportsId != null && !rawSportsId.isBlank()) {
            candidates.add(rawSportsId);
        }
        String rawEventType = request.getParameter("eventType");
        if (rawEventType != null && !rawEventType.isBlank()) {
            candidates.add(rawEventType);
        }

        String[] keys = { "sports_id", "sports_id[]", "sportsId", "sportsId[]", "sports_id[0]", "eventType",
                "eventType[]", "event_type", "event_type[]", "eventTypes", "eventTypes[]" };
        for (String key : keys) {
            String[] vals = request.getParameterValues(key);
            if (vals != null) {
                for (String v : vals) {
                    if (v != null && !v.isBlank()) {
                        candidates.add(v);
                    }
                }
            }
        }

        for (String raw : candidates) {
            String str = raw.trim();
            if (str.startsWith("[") && str.endsWith("]")) {
                str = str.substring(1, str.length() - 1);
            }
            if (str.isBlank())
                continue;

            String[] parts = str.split(",");
            for (String p : parts) {
                String cleaned = p.trim().replaceAll("[\"']", "");
                if (!cleaned.isBlank()) {
                    try {
                        int id = Integer.parseInt(cleaned);
                        if (!result.contains(id)) {
                            result.add(id);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return result;
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

    @GetMapping({ "/stats/by-event-type", "/report/event-types" })
    @Operation(summary = "Cantidad de reservas por cada tipo de evento", description = "Retorna la cantidad de reservas asociadas a cada tipo de evento. Aunque una reserva incluya múltiples tipos de evento, se contabiliza individualmente para cada tipo.")
    public ResponseEntity<List<com.sportsbooking.mainapi.dto.booking.response.EventTypeBookingCountDto>> getBookingsCountByEventType(
            @RequestParam(name = "venueId", required = false) UUID venueId,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<com.sportsbooking.mainapi.dto.booking.response.EventTypeBookingCountDto> response = bookingService
                .getBookingsCountByEventType(venueId, status, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping({ "/admin/event-types-count", "/admin/stats/event-types" })
    @PreAuthorize("hasAnyRole('ROLE_CLIENT', 'ROLE_ADMIN')")
    @Operation(summary = "Total de tipos de eventos de reservas para administración", description = "Retorna el total de reservas agrupadas por tipo de evento de todos los venues en general (ejm: { \"Bodas\": 5, \"Cumpleaños\": 6 }). Aún si una reserva tiene varios eventos, se suma cada tipo de evento.")
    public ResponseEntity<Map<String, Long>> getAdminEventTypesCount() {
        return ResponseEntity.ok(bookingService.getAdminEventTypesCount());
    }
}
