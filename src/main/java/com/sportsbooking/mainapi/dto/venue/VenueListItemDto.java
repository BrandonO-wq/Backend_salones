package com.sportsbooking.mainapi.dto.venue;

import lombok.Data;

import java.util.UUID;

/**
 * DTO de resultado MyBatis para listados de venues.
 *
 * <p>Usado como {@code resultType} en {@code venue-mapper.xml}
 * para las queries {@code findInBounds} y {@code searchVenues}.
 *
 * <p>Incluye rating promedio y primera foto para visualización en mapa/lista.
 */
@Data
public class VenueListItemDto {

    private UUID id;
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    private String ubigeo;
    private Boolean illuminated;
    private String dimensions;

    /** Precio de día (schedule_type = 1) */
    private Double priceDay;

    /** Precio de noche (schedule_type = 2) */
    private Double priceNight;

    /** Precio mínimo extraído desde available_venues (dia/noche) */
    private Double minPrice;

    /** IDs de deportes (devuelto como array PG, mapeado por MyBatis). */
    private Integer[] sports;

    /** IDs de superficies. */
    private Integer[] surfaces;

    /** Promedio de puntuación (1-5). 0 si no tiene reviews. */
    private Double avgRating;

    /** Cantidad de reviews. */
    private Long reviewCount;

    /** Capacidad máxima del salón. */
    private Integer maxCapacity;

    /** Precio base del salón (si aplica). */
    private Double basePrice;

    /** URL de la primera foto del venue (null si no tiene). */
    private String photoUrl;
}
