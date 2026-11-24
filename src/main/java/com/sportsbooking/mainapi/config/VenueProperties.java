package com.sportsbooking.mainapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Propiedades configurables para el módulo de venues.
 *
 * <p>
 * Mapeadas desde {@code app.venue.*} en {@code application.yml}.
 *
 * <p>
 * Ejemplo de configuración:
 * 
 * <pre>{@code
 * app:
 *   venue:
 *     max-search-radius-km: 15
 * }</pre>
 */
@Getter
@Setter
// @Configuration
@ConfigurationProperties(prefix = "app.venue")
public class VenueProperties {

    /**
     * Radio máximo de búsqueda PostGIS en kilómetros.
     * Se convierte a metros internamente para {@code ST_DWithin}.
     * Valor por defecto: 15 km.
     */
    private int maxSearchRadiusKm = 15;

    /** Configuración de imágenes de venues. */
    private Images images = new Images();

    /**
     * Retorna el radio máximo en metros (para uso directo en ST_DWithin).
     *
     * @return radio en metros.
     */
    public int getMaxSearchRadiusMeters() {
        return maxSearchRadiusKm * 1000;
    }

    /**
     * Configuración de almacenamiento de imágenes de venues.
     */
    @Getter
    @Setter
    public static class Images {

        /** Carpeta base en disco donde se almacenan las imágenes. */
        // private String basePath = "./uploads/venues";
        private String basePath = "/home/brandon/eventplace/venues";

        /** Tamaño máximo de archivo en MB. */
        private int maxSizeMb = 5;

        /** Cantidad máxima de fotos por venue. */
        private int maxCount = 5;
    }
}
