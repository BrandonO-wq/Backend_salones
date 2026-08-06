package com.sportsbooking.mainapi.controller;

import com.sportsbooking.mainapi.dto.common.ErrorResponse;
import com.sportsbooking.mainapi.dto.common.MessageResponse;
import com.sportsbooking.mainapi.dto.notification.NotifyDebtRequest;
import com.sportsbooking.mainapi.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Admin Users", description = "Operaciones de administración sobre usuarios")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final NotificationService notificationService;

    @Operation(
            summary = "Notificar deuda al usuario",
            description = "Envía una notificación push y registra una alerta en base de datos para informar de falta de pago al usuario especificado.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "Notificación de deuda enviada exitosamente", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "No autorizado (requiere rol ADMIN)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/{userId}/notify-debt")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<MessageResponse> notifyDebt(
            @PathVariable("userId") UUID userId,
            @Valid @RequestBody NotifyDebtRequest request) {
        notificationService.notifyDebt(userId, request.title(), request.body());
        return ResponseEntity.ok(new MessageResponse("Notificación de deuda enviada exitosamente."));
    }
}
