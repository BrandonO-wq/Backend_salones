package com.sportsbooking.mainapi.controller;

import com.sportsbooking.mainapi.dto.commission.VenueCommissionResponse;
import com.sportsbooking.mainapi.jpa.entity.HostDebtResponseDto;
import com.sportsbooking.mainapi.service.CommissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/host-debts")
@RequiredArgsConstructor
public class HostDebtsController {

    private final CommissionService commissionService;

    @GetMapping
    public ResponseEntity<List<HostDebtResponseDto>> getMyHostDebts(@AuthenticationPrincipal UUID hostId) {
        return ResponseEntity.ok(commissionService.getHostDebts(hostId));
    }

    @GetMapping("/venue/{venueId}/commission")
    public ResponseEntity<VenueCommissionResponse> getVenueCurrentCommission(@PathVariable UUID venueId) {
        return ResponseEntity.ok(commissionService.calculateCurrentCycleCommission(venueId));
    }
}
