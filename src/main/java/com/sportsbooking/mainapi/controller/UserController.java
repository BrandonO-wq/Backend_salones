package com.sportsbooking.mainapi.controller;

import com.sportsbooking.mainapi.dto.common.ErrorResponse;
import com.sportsbooking.mainapi.dto.common.MessageResponse;
import com.sportsbooking.mainapi.dto.user.request.UpdateProfileRequest;
import com.sportsbooking.mainapi.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador REST para operaciones sobre el perfil del usuario autenticado.
 *
 * <p>
 * Prefijo de ruta: {@code /users}.
 *
 * <p>
 * Todos los endpoints requieren un JWT válido (Bearer token).
 * El {@code userId} se extrae del {@link Authentication} — nunca del body.
 */
@Tag(name = "Users", description = "Gestión del perfil de usuario")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

        private final UserService userService;

        // ─────────────────────────────────────────────
        // PUT /users/profile
        // ─────────────────────────────────────────────

        /**
         * Actualiza el nombre completo y el teléfono del usuario autenticado.
         *
         * @param request        nuevos valores de nombre y teléfono.
         * @param authentication contexto de seguridad con el UUID del usuario.
         * @return mensaje de confirmación.
         */
        @Operation(summary = "Actualizar perfil", description = "Actualiza parcialmente el perfil del usuario autenticado. Solo se modifican los campos enviados.", security = @SecurityRequirement(name = "bearerAuth"), responses = {
                        @ApiResponse(responseCode = "200", description = "Perfil actualizado", content = @Content(schema = @Schema(implementation = MessageResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        })
        @PatchMapping(value = "/profile", consumes = { "application/json", "application/merge-patch+json" })
        public ResponseEntity<MessageResponse> updateProfile(
                        @Valid @RequestBody UpdateProfileRequest request,
                        @AuthenticationPrincipal UUID userId) {
                MessageResponse response = userService.updateProfile(userId, request);
                return ResponseEntity.ok(response);
        }
}
