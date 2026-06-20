package com.sportsbooking.mainapi.controller;

import com.sportsbooking.mainapi.dto.parameter.ParameterDto;
import com.sportsbooking.mainapi.service.ParameterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para consulta de parámetros del sistema.
 *
 * <p>
 * Prefijo de ruta: {@code /parameters}.
 * Todos los endpoints son públicos (no requieren autenticación).
 */
@Tag(name = "Parameters", description = "Catálogo de parámetros del sistema")
@RestController
@RequestMapping("/parameters")
@RequiredArgsConstructor
public class ParameterController {

    private final ParameterService parameterService;

    /**
     * Retorna los parámetros hijos activos de una categoría padre.
     *
     * <p>
     * Categorías padre: 1=Deportes, 2=Superficies, 3=Comodidades,
     * 4=Bancos, 5=Billeteras Digitales.
     *
     * @param parentId ID del parámetro padre.
     * @return lista de parámetros hijos activos (cacheados en Redis 12h).
     */
    @Operation(summary = "Listar parámetros por categoría", description = "Retorna parámetros hijos activos de un padre. Cache Redis 12h.")
    @GetMapping("/{parentId}")
    public ResponseEntity<List<ParameterDto>> getByParent(@PathVariable("parentId") int parentId) {
        return ResponseEntity.ok(parameterService.getByParent(parentId));
    }
}
