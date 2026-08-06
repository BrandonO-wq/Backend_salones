package com.sportsbooking.mainapi.controller;

import com.sportsbooking.mainapi.dto.user.response.HostDetailResponseDto;
import com.sportsbooking.mainapi.service.HostDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Host Detail", description = "Endpoint para obtener información detallada del Host (Propietario)")
@RestController
@RequestMapping("/host-detail")
@RequiredArgsConstructor
public class HostDetailController {

    private final HostDetailService hostDetailService;

    @Operation(summary = "Obtener detalles del Host", description = "Retorna el perfil del host, cantidad de salones, reservas, calificación promedio y estado de deuda.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_CLIENT', 'ROLE_ADMIN')")
    public ResponseEntity<HostDetailResponseDto> getHostDetail(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(hostDetailService.getHostDetail(id));
    }
}
