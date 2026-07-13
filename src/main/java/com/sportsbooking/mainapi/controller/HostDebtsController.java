package com.sportsbooking.mainapi.controller;

import com.sportsbooking.mainapi.dto.commission.HostDebtorResponseDto;
import com.sportsbooking.mainapi.dto.commission.VenueCommissionResponse;
import com.sportsbooking.mainapi.dto.commission.RegisteredHostResponseDto;
import com.sportsbooking.mainapi.jpa.entity.HostDebtResponseDto;
import com.sportsbooking.mainapi.service.CommissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
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
}
