package com.sportsbooking.mainapi.controller;

import com.sportsbooking.mainapi.dto.commission.HostDebtorResponseDto;
import com.sportsbooking.mainapi.dto.commission.VenueCommissionResponse;
import com.sportsbooking.mainapi.dto.commission.RegisteredHostResponseDto;
import com.sportsbooking.mainapi.dto.commission.VenueDebtReportResponseDto;
import com.sportsbooking.mainapi.jpa.entity.HostDebtResponseDto;
import com.sportsbooking.mainapi.service.CommissionService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.sportsbooking.mainapi.dto.common.MessageResponse;
import com.sportsbooking.mainapi.dto.commission.ConfirmCommissionChangeRequest;
import com.sportsbooking.mainapi.dto.commission.ConfirmCycleConfigChangeRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/host-debts")
@RequiredArgsConstructor
public class HostDebtsController {

    private final CommissionService commissionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_CLIENT', 'ROLE_ADMIN')")
    public ResponseEntity<List<HostDebtResponseDto>> getHostDebts(
            @RequestParam(required = false) UUID hostId,
            @AuthenticationPrincipal UUID loggedInUserId,
            Authentication authentication) {

        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN") || auth.getAuthority().equals("ADMIN"));

        UUID targetHostId = isAdmin ? hostId : loggedInUserId;
        return ResponseEntity.ok(commissionService.getHostDebts(targetHostId));
    }

    @GetMapping("/venue/{venueId}/commission")
    @PreAuthorize("hasAnyRole('ROLE_CLIENT', 'ROLE_ADMIN')")
    public ResponseEntity<VenueCommissionResponse> getVenueCurrentCommission(@PathVariable UUID venueId) {
        return ResponseEntity.ok(commissionService.calculateCurrentCycleCommission(venueId));
    }

    @GetMapping("/debtors")
    @PreAuthorize("hasAnyRole('ROLE_CLIENT', 'ROLE_ADMIN')")
    public ResponseEntity<List<HostDebtorResponseDto>> getHostDebtors() {
        return ResponseEntity.ok(commissionService.getHostDebtors());
    }

    @GetMapping("/registered-hosts")
    @PreAuthorize("hasAnyRole('ROLE_CLIENT', 'ROLE_ADMIN')")
    public ResponseEntity<List<RegisteredHostResponseDto>> getRegisteredHosts() {
        return ResponseEntity.ok(commissionService.getRegisteredHosts());
    }

    @GetMapping("/commission/percentage")
    @PreAuthorize("hasAnyRole('ROLE_CLIENT', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, Double>> getCommissionPercentage() {
        Double value = commissionService.getCommissionPercentage();
        return ResponseEntity.ok(Map.of("value", value));
    }

    @GetMapping("/venues/report")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<VenueDebtReportResponseDto>> getVenueDebtReport() {
        return ResponseEntity.ok(commissionService.getVenueDebtReport());
    }

    @PostMapping("/commission/request-change")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<MessageResponse> requestCommissionChange(
            @AuthenticationPrincipal UUID adminUserId) {
        return ResponseEntity.ok(commissionService.requestCommissionChange(adminUserId));
    }

    @PostMapping("/commission/confirm-change")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<MessageResponse> confirmCommissionChange(
            @AuthenticationPrincipal UUID adminUserId,
            @Valid @RequestBody ConfirmCommissionChangeRequest request) {
        return ResponseEntity.ok(commissionService.confirmCommissionChange(
                adminUserId, request.otpCode(), request.newValue()));
    }

    @PostMapping("/venue/{venueId}/toggle-status")
    @Tag(name = "Host Debts", description = "Endpoint para activar/desactivar un venue")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<MessageResponse> toggleVenueStatus(
            @PathVariable UUID venueId,
            @RequestParam boolean active) {
        return ResponseEntity.ok(commissionService.toggleVenueStatus(venueId, active));
    }

    @PostMapping("/venue/{venueId}/enable")
    @Tag(name = "Host Debts", description = "Endpoint para habilitar un venue")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<MessageResponse> enableVenue(
            @PathVariable UUID venueId) {
        return ResponseEntity.ok(commissionService.enableVenue(venueId));
    }

    @GetMapping("/venue/{venueId}/disabled-status")
    @Tag(name = "Host Debts", description = "Endpoint para obtener el estado de inhabilitación de un venue")
    @PreAuthorize("hasAnyRole('ROLE_CLIENT', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getVenueDisabledStatus(
            @PathVariable UUID venueId) {
        return ResponseEntity.ok(commissionService.getVenueDisabledStatus(venueId));
    }

    @GetMapping("/commission/cycle-config")
    @Tag(name = "Host Debts", description = "Endpoint para obtener la configuración del ciclo de facturación y pagos")
    @PreAuthorize("hasAnyRole('ROLE_CLIENT', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getCycleConfig() {
        return ResponseEntity.ok(commissionService.getCycleConfig());
    }

    @PostMapping("/commission/request-cycle-change")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<MessageResponse> requestCycleConfigChange(
            @AuthenticationPrincipal UUID adminUserId) {
        return ResponseEntity.ok(commissionService.requestCycleConfigChange(adminUserId));
    }

    @PostMapping("/commission/confirm-cycle-change")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<MessageResponse> confirmCycleConfigChange(
            @AuthenticationPrincipal UUID adminUserId,
            @Valid @RequestBody ConfirmCycleConfigChangeRequest request) {
        return ResponseEntity.ok(commissionService.confirmCycleConfigChange(adminUserId, request));
    }
}
