package com.sportsbooking.mainapi.dto.venue.request;

import java.util.List;

/**
 * Request para búsqueda avanzada de venues.
 *
 * <p>Todos los filtros son opcionales. Los que se envíen se aplican con AND.
 */
public record SearchVenuesRequest(

        /** ID del deporte a filtrar. */
        Integer sportId,

        /** IDs de superficies a filtrar (filtra venues que tengan al menos una). */
        List<Integer> surfaceIds,

        /** Filtrar por iluminación. */
        Boolean isIlluminated,

        /** Texto libre para buscar en nombre y dirección. */
        String keyword,

        /** Latitud del usuario (para filtro por cercanía). */
        Double latitude,

        /** Longitud del usuario (para filtro por cercanía). */
        Double longitude,

        /** Radio de búsqueda en km (si no se envía, se usa el máximo configurado). */
        Integer radiusKm
) {}
