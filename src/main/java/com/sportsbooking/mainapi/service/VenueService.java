package com.sportsbooking.mainapi.service;

import com.sportsbooking.mainapi.dto.common.MessageResponse;
import com.sportsbooking.mainapi.dto.venue.VenueListItemDto;
import com.sportsbooking.mainapi.dto.venue.request.CreateVenueRequest;
import com.sportsbooking.mainapi.dto.venue.request.InBoundsRequest;
import com.sportsbooking.mainapi.dto.venue.request.SearchVenuesRequest;
import com.sportsbooking.mainapi.dto.venue.request.UpdateVenueRequest;
import com.sportsbooking.mainapi.dto.venue.response.VenueDetailResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Contrato de negocio para el módulo de venues.
 */
public interface VenueService {

    /**
     * Crea un venue con sus horarios y datos de pago.
     *
     * @param userId  ID del usuario autenticado (propietario).
     * @param request datos del venue a crear.
     * @return detalle completo del venue creado.
     */
    VenueDetailResponse createVenue(UUID userId, CreateVenueRequest request);

    /**
     * Obtiene el detalle completo de un venue por su ID.
     *
     * @param venueId ID del venue.
     * @return detalle completo del venue.
     */
    VenueDetailResponse getVenueById(UUID venueId);

    /**
     * Actualiza parcialmente un venue.
     *
     * @param userId  ID del usuario autenticado (propietario).
     * @param venueId ID del venue a actualizar.
     * @param request campos a actualizar (solo los no-null se aplican).
     * @return detalle completo del venue actualizado.
     */
    VenueDetailResponse updateVenue(UUID userId, UUID venueId, UpdateVenueRequest request);

    /**
     * Soft-delete de un venue (is_active = false).
     *
     * @param userId  ID del usuario autenticado (propietario).
     * @param venueId ID del venue a desactivar.
     * @return mensaje de confirmación.
     */
    MessageResponse deleteVenue(UUID userId, UUID venueId);

    /**
     * Busca venues dentro de un radio geográfico usando PostGIS.
     *
     * @param request Filtros y coordenadas geográficas (lat, lng, radiusKm, etc).
     * @return lista de venues ordenados por cercanía.
     */
    List<VenueListItemDto> findVenuesInBounds(InBoundsRequest request);

    /**
     * Búsqueda avanzada de venues con filtros.
     *
     * @param request filtros de búsqueda.
     * @return lista de venues que cumplen los filtros.
     */
    List<VenueListItemDto> searchVenues(SearchVenuesRequest request);

    /**
     * Sube fotos de un venue (multipart). Valida, optimiza y almacena en disco.
     *
     * @param userId  ID del usuario autenticado (propietario).
     * @param venueId ID del venue.
     * @param files   archivos de imagen (multipart).
     * @return mensaje de confirmación.
     */
    MessageResponse uploadPhotos(UUID userId, UUID venueId, MultipartFile[] files);

    /**
     * Obtiene un recurso de imagen de venue por su ID de registro.
     *
     * @param photoId UUID del registro en venue_photos.
     * @return ResponseEntity con el recurso de imagen y content-type apropiados.
     */
    ResponseEntity<Resource> getPhotoResource(UUID photoId);

    /**
     * Retorna todos los venues activos del usuario autenticado (host).
     *
     * @param userId ID del usuario HOST.
     * @return lista de venues con rating y foto.
     */
    List<VenueListItemDto> getMyVenues(UUID userId);

    /**
     * Elimina permanentemente una foto registrada para un venue.
     *
     * @param userId  ID del usuario autenticado (propietario).
     * @param photoId UUID del registro en venue_photos.
     * @return mensaje de confirmación.
     */
    MessageResponse deletePhoto(UUID userId, UUID photoId);
}
