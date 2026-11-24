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

    /** Precio base del salón (desde venues.base_price). */
    private Double basePrice;

    /** Precio de hora extra (desde available_venues.precio_hora_extra). */
    private Double extraHourPrice;

    /** Precio mínimo extraído desde available_venues */
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

    /** URL de la primera foto del venue (null si no tiene). */
    private String photoUrl;
}
