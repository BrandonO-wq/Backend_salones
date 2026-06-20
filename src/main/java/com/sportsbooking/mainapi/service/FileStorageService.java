package com.sportsbooking.mainapi.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Servicio de almacenamiento de archivos en disco.
 *
 * <p>Responsable de validar, optimizar y almacenar imágenes
 * en el filesystem, usando nombres UUIDv7 para evitar duplicados.
 */
public interface FileStorageService {

    /**
     * Almacena un archivo de imagen validado y optimizado.
     *
     * @param file    archivo multipart recibido.
     * @param venueId ID del venue al que pertenece la imagen.
     * @return nombre del archivo generado (UUIDv7 + extensión).
     * @throws IllegalArgumentException si el archivo no es válido.
     */
    String store(MultipartFile file, UUID venueId);

    /**
     * Carga un archivo como {@link Resource} para descarga.
     *
     * @param venueId  ID del venue.
     * @param filename nombre del archivo (guardado en {@code venue_photos.url}).
     * @return resource del archivo.
     * @throws com.sportsbooking.mainapi.common.exception.ResourceNotFoundException si no existe.
     */
    Resource load(UUID venueId, String filename);

    /**
     * Elimina un archivo físicamente del disco.
     *
     * @param venueId  ID del venue.
     * @param filename nombre del archivo.
     */
    void delete(UUID venueId, String filename);
}
