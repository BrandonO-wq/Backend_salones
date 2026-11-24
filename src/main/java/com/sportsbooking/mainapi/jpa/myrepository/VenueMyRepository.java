package com.sportsbooking.mainapi.jpa.myrepository;

import com.sportsbooking.mainapi.dto.venue.VenueListItemDto;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio MyBatis para queries complejas de venues.
 *
 * <p>Los mappers XML se encuentran en {@code sqlmapPg/mapper/venue-mapper.xml}.
 */
@Repository
public interface VenueMyRepository {

    /**
     * Busca venues activos dentro de un radio desde un punto geográfico.
     *
     * @param lat          latitud del punto central.
     * @param lng          longitud del punto central.
     * @param radiusMeters radio de búsqueda en metros.
     * @param sportId      ID de deporte opcional.
     * @return lista de venues ordenados por cercanía.
     */
    List<VenueListItemDto> findInBounds(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMeters") int radiusMeters,
            @Param("sportId") Integer sportId,
            @Param("venueName") String venueName,
            @Param("surfaceId") Integer surfaceId,
            @Param("hasLighting") Boolean hasLighting,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );

    /**
     * Búsqueda avanzada con filtros opcionales.
     *
     * @param sportId       ID del deporte (nullable).
     * @param surfaceIds    IDs de superficies (nullable, filtra overlap con array del venue).
     * @param isIlluminated filtro de iluminación (nullable).
     * @param keyword       texto libre para nombre/dirección (nullable).
     * @param lat           latitud para ordenar por cercanía (nullable).
     * @param lng           longitud para ordenar por cercanía (nullable).
     * @param radiusMeters  radio máximo en metros (nullable, requiere lat/lng).
     * @return lista de venues que cumplen los filtros.
     */
    List<VenueListItemDto> searchVenues(
            @Param("sportId") Integer sportId,
            @Param("surfaceIds") Integer[] surfaceIds,
            @Param("isIlluminated") Boolean isIlluminated,
            @Param("keyword") String keyword,
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radiusMeters") Integer radiusMeters
    );

    /**
     * Retorna todos los venues activos de un host con rating y foto.
     *
     * @param hostUserId ID del usuario HOST.
     * @return lista de venues del host ordenados por fecha de creación desc.
     */
    List<VenueListItemDto> findByHostUserId(@Param("hostUserId") UUID hostUserId);
}
