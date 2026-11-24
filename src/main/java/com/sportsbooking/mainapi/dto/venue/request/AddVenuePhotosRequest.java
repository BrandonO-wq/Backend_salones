package com.sportsbooking.mainapi.dto.venue.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request para agregar fotos a un venue.
 */
public record AddVenuePhotosRequest(

        @NotEmpty(message = "Debe enviar al menos una URL de foto")
        List<String> urls
) {}
