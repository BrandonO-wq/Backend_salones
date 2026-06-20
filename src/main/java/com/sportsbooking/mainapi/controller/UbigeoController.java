package com.sportsbooking.mainapi.controller;

import com.sportsbooking.mainapi.dto.ubigeo.UbigeoTreeResponse;
import com.sportsbooking.mainapi.service.UbigeoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para consulta de ubigeos del Perú.
 *
 * <p>Endpoint público. Cache Redis 24h.
 */
@Tag(name = "Ubigeos", description = "Ubigeos de Perú (Departamento → Provincia → Distrito)")
@RestController
@RequestMapping("/ubigeos")
@RequiredArgsConstructor
public class UbigeoController {

    private final UbigeoService ubigeoService;

    /**
     * Retorna el árbol completo de ubigeos del Perú.
     *
     * <p>Estructura jerárquica: Departamento (2 dígitos) → Provincia (4 dígitos)
     * → Distrito (6 dígitos). Cacheado en Redis con TTL de 24 horas.
     *
     * @return árbol completo de ubigeos.
     */
    @Operation(summary = "Obtener árbol de ubigeos",
            description = "Retorna árbol jerárquico completo: Depto → Prov → Dist. Cache Redis 24h.")
    @GetMapping
    public ResponseEntity<UbigeoTreeResponse> getTree() {
        return ResponseEntity.ok(ubigeoService.getTree());
    }
}
