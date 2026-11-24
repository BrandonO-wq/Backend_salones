package com.sportsbooking.mainapi.controller;

import com.sportsbooking.mainapi.dto.commission.HostDebtPaymentRequest;
import com.sportsbooking.mainapi.dto.commission.HostDebtPaymentResponse;
import com.sportsbooking.mainapi.jpa.entity.HostDebtPayment;
import com.sportsbooking.mainapi.service.HostDebtPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/host-debts/payments")
@RequiredArgsConstructor
@Tag(name = "Host Debt Payments", description = "Endpoints para el registro y gestión de pagos de deudas de anfitriones")
public class HostDebtPaymentController {

    private final HostDebtPaymentService hostDebtPaymentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_CLIENT', 'ROLE_ADMIN')")
    @Operation(summary = "Registrar un pago de deuda de comisión de anfitrión")
    public ResponseEntity<HostDebtPayment> registerPayment(
            @Valid @RequestBody HostDebtPaymentRequest request) {
        HostDebtPayment payment = hostDebtPaymentService.registerPayment(request);
        return ResponseEntity.ok(payment);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_CLIENT','ROLE_ADMIN')")
    @Operation(summary = "Obtener los pagos de comisión por propietario (anfitrión)")
    public ResponseEntity<List<HostDebtPaymentResponse>> getPayments(
            @RequestParam(name = "hostId", required = false) UUID hostId,
            @AuthenticationPrincipal UUID loggedInUserId,
            Authentication authentication) {

        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN") || auth.getAuthority().equals("ADMIN"));

        UUID targetHostId = isAdmin ? hostId : loggedInUserId;
        return ResponseEntity.ok(hostDebtPaymentService.getPayments(targetHostId));
    }
}
