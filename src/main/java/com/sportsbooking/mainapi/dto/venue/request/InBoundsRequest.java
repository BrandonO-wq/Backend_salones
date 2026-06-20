package com.sportsbooking.mainapi.dto.venue.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Request para buscar venues activos dentro de un radio geográfico.
 */
public record InBoundsRequest(
        
        @NotNull(message = "La latitud es obligatoria")
        Double lat,
        
        @NotNull(message = "La longitud es obligatoria")
        Double lng,
        
        /**
         * Radio de búsqueda en kilómetros.
         * Si es null, el servicio aplicará un valor por defecto (ej. 10km).
         */
        Integer radiusKm,
        
        /**
         * Filtro opcional por deporte.
         */
        Integer sportId,
        
        /**
         * Filtro opcional por coincidencia de nombre.
         */
        String venueName,
        
        /**
         * Filtro opcional por ID de superficie.
         */
        Integer surfaceId,
        
        /**
         * Filtro opcional por iluminación (true = con luz, null = todas).
         */
        Boolean hasLighting,
        
        @Min(value = 0, message = "El precio mínimo no puede ser negativo")
        BigDecimal minPrice,
        
        @Schema(description = "Precio máximo por hora", example = "150.0")
        @Min(value = 0, message = "El precio máximo no puede ser negativo")
        BigDecimal maxPrice
) {}
