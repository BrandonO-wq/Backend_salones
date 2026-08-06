package com.sportsbooking.mainapi.controller;

import com.sportsbooking.mainapi.dto.common.ErrorResponse;
import com.sportsbooking.mainapi.dto.common.MessageResponse;
import com.sportsbooking.mainapi.dto.venue.VenueListItemDto;
import com.sportsbooking.mainapi.dto.venue.request.CreateVenueRequest;
import com.sportsbooking.mainapi.dto.venue.request.InBoundsRequest;
import com.sportsbooking.mainapi.dto.venue.request.SearchVenuesRequest;
import com.sportsbooking.mainapi.dto.venue.request.UpdateVenueRequest;
import com.sportsbooking.mainapi.dto.venue.response.VenueBookingReportDto;
import com.sportsbooking.mainapi.dto.venue.response.VenueStatsResponseDto;
import com.sportsbooking.mainapi.dto.venue.response.VenueDetailedReportDto;
import com.sportsbooking.mainapi.dto.venue.response.VenueDetailResponse;
import com.sportsbooking.mainapi.dto.venue.response.HostVenueResponseDto;
import org.springframework.security.access.prepost.PreAuthorize;
import com.sportsbooking.mainapi.service.VenueService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para el módulo de venues (canchas).
 *
 * <p>
 * Prefijo de ruta: {@code /venues}.
 *
 * <p>
 * Endpoints públicos: {@code GET /{id}}, {@code GET /in-bounds},
 * {@code POST /search}, {@code GET /photos/{photoId}}.
 * <br>
 * Endpoints protegidos (JWT): {@code POST}, {@code PATCH}, {@code DELETE},
 * {@code POST /{id}/photos}.
 */
@Tag(name = "Venues", description = "Gestión de canchas / locales deportivos")
@RestController
@RequestMapping("/venues")
@RequiredArgsConstructor
public class VenueController {

        private final VenueService venueService;

        // ─────────────────────────────────────────────
        // GET /venues/my (venues del host autenticado)
        // ─────────────────────────────────────────────

        /**
         * Lista los venues del usuario autenticado (host).
         */
        @Operation(summary = "Mis venues", description = "Retorna los venues activos del host autenticado con rating y foto.", security = @SecurityRequirement(name = "bearerAuth"), responses = {
                        @ApiResponse(responseCode = "200", description = "Lista de venues del host")
        })
        @GetMapping("/my")
        public ResponseEntity<List<VenueListItemDto>> getMyVenues(@AuthenticationPrincipal UUID userId) {
                return ResponseEntity.ok(venueService.getMyVenues(userId));
        }

        // ─────────────────────────────────────────────
        // POST /venues
        // ─────────────────────────────────────────────

        /**
         * Crea un nuevo venue con horarios y datos de pago.
         */
        @Operation(summary = "Crear venue", description = "Crea un venue con sus horarios disponibles y datos de pago.", security = @SecurityRequirement(name = "bearerAuth"), responses = {
                        @ApiResponse(responseCode = "201", description = "Venue creado", content = @Content(schema = @Schema(implementation = VenueDetailResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        })
        @PostMapping
        public ResponseEntity<VenueDetailResponse> createVenue(
                        @Valid @RequestBody CreateVenueRequest request,
                        @AuthenticationPrincipal UUID userId) {
                if (userId == null) {
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                                        "No autorizado. Proporciona un token JWT válido en Authorization header.");
                }
                VenueDetailResponse response = venueService.createVenue(userId, request);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        // ─────────────────────────────────────────────
        // GET /venues/{id} (público)
        // ─────────────────────────────────────────────

        /**
         * Obtiene el detalle completo de un venue.
         */
        @Operation(summary = "Obtener venue", description = "Retorna el detalle completo de un venue con horarios, pagos, fotos y rating.", responses = {
                        @ApiResponse(responseCode = "200", description = "Venue encontrado", content = @Content(schema = @Schema(implementation = VenueDetailResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Venue no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        })

        // @GetMapping("/{id}")
        // public ResponseEntity<VenueDetailResponse> getVenue(@PathVariable UUID id) {
        // return ResponseEntity.ok(venueService.getVenueById(id));
        // }
        @GetMapping("/{id}")
        public ResponseEntity<VenueDetailResponse> getVenue(
                        @PathVariable("id") UUID id) {
                System.out.println(
                                "////////////////////////////////////////////////////////////////////////////////////////////////////////");
                System.out.println(
                                "////////////////////////////////////////////////////////////////////////////////////////////////////////");
                System.out.println(
                                "////////////////////////////////////////////////////////////////////////////////////////////////////////");
                System.out.println(
                                "////////////////////////////////////////////////////////////////////////////////////////////////////////");
                System.out.println("ID recibido: " + id);

                return ResponseEntity.ok(venueService.getVenueById(id));
        }

        // ─────────────────────────────────────────────
        // PATCH /venues/{id}
        // ─────────────────────────────────────────────

        /**
         * Actualiza parcialmente un venue (solo el propietario).
         */
        @Operation(summary = "Actualizar venue", description = "Actualiza parcialmente un venue. Solo los campos enviados se modifican.", security = @SecurityRequirement(name = "bearerAuth"), responses = {
                        @ApiResponse(responseCode = "200", description = "Venue actualizado", content = @Content(schema = @Schema(implementation = VenueDetailResponse.class))),
                        @ApiResponse(responseCode = "403", description = "No es el propietario", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Venue no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        })
        @PatchMapping("/{id}")
        public ResponseEntity<VenueDetailResponse> updateVenue(
                        @PathVariable("id") UUID id,
                        @Valid @RequestBody UpdateVenueRequest request,
                        @AuthenticationPrincipal UUID userId) {
                return ResponseEntity.ok(venueService.updateVenue(userId, id, request));
        }

        // ─────────────────────────────────────────────
        // DELETE /venues/{id}
        // ─────────────────────────────────────────────

        /**
         * Soft-delete de un venue (solo el propietario).
         */
        @Operation(summary = "Eliminar venue", description = "Desactiva un venue (soft delete). Solo el propietario puede eliminarlo.", security = @SecurityRequirement(name = "bearerAuth"), responses = {
                        @ApiResponse(responseCode = "200", description = "Venue eliminado", content = @Content(schema = @Schema(implementation = MessageResponse.class))),
                        @ApiResponse(responseCode = "403", description = "No es el propietario"),
                        @ApiResponse(responseCode = "404", description = "Venue no encontrado")
        })
        @DeleteMapping("/{id}")
        public ResponseEntity<MessageResponse> deleteVenue(
                        @PathVariable("id") UUID id,
                        @AuthenticationPrincipal UUID userId) {
                return ResponseEntity.ok(venueService.deleteVenue(userId, id));
        }

        // ─────────────────────────────────────────────
        // POST /venues/in-bounds (público)
        // ─────────────────────────────────────────────

        /**
         * Busca venues cercanos usando PostGIS.
         *
         * @param request Filtros geográficos y avanzados (latitud, longitud, límite de
         *                radio y filtros opcionales).
         */
        @Operation(summary = "Buscar venues por cercanía", description = "Retorna venues activos dentro de un radio geográfico usando PostGIS.", responses = {
                        @ApiResponse(responseCode = "200", description = "Lista de venues cercanos")
        })
        @PostMapping("/in-bounds")
        public ResponseEntity<List<VenueListItemDto>> findInBounds(
                        @Valid @RequestBody InBoundsRequest request) {
                return ResponseEntity.ok(venueService.findVenuesInBounds(request));
        }

        // ─────────────────────────────────────────────
        // POST /venues/search (público)
        // ─────────────────────────────────────────────

        /**
         * Búsqueda avanzada de venues con filtros opcionales.
         */
        @Operation(summary = "Buscar venues con filtros", description = "Búsqueda avanzada con filtros opcionales: deporte, superficie, iluminación, keyword, ubicación.", responses = {
                        @ApiResponse(responseCode = "200", description = "Lista de venues filtrados")
        })
        @PostMapping("/search")
        public ResponseEntity<List<VenueListItemDto>> searchVenues(
                        @RequestBody SearchVenuesRequest request) {
                return ResponseEntity.ok(venueService.searchVenues(request));
        }

        // ─────────────────────────────────────────────
        // POST /venues/{id}/photos (multipart upload)
        // ─────────────────────────────────────────────

        /**
         * Sube imágenes de un venue. Acepta multipart/form-data.
         *
         * <p>
         * Valida extensión, content-type real, tamaño. Optimiza compresión.
         * Genera nombre UUIDv7 por archivo. Máximo configurado en yml (default 5).
         *
         * @param id    ID del venue.
         * @param files array de archivos de imagen.
         */
        @Operation(summary = "Subir fotos del venue", description = "Sube imágenes (multipart). Valida tipo, tamaño (max 5MB), optimiza y almacena.", security = @SecurityRequirement(name = "bearerAuth"), responses = {
                        @ApiResponse(responseCode = "201", description = "Fotos subidas", content = @Content(schema = @Schema(implementation = MessageResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Archivo inválido"),
                        @ApiResponse(responseCode = "403", description = "No es el propietario"),
                        @ApiResponse(responseCode = "404", description = "Venue no encontrado")
        })
        @PostMapping(value = "/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<MessageResponse> uploadPhotos(
                        @PathVariable("id") UUID id,
                        @RequestParam("files") MultipartFile[] files,
                        @AuthenticationPrincipal UUID userId) {
                return ResponseEntity.status(HttpStatus.CREATED).body(
                                venueService.uploadPhotos(userId, id, files));
        }

        // ─────────────────────────────────────────────
        // GET /venues/photos/{photoId} (público)
        // ─────────────────────────────────────────────

        /**
         * Obtiene una imagen de venue por su ID de registro.
         *
         * <p>
         * Busca el registro en {@code venue_photos} por UUID, reconstruye
         * la ruta del archivo en disco y lo retorna como stream. Nunca expone
         * la ruta interna del servidor.
         *
         * @param photoId UUID del registro en venue_photos.
         * @return archivo de imagen como stream.
         */
        @Operation(summary = "Obtener foto de venue", description = "Retorna una imagen de venue por su ID. Acceso público, seguro por UUID.", responses = {
                        @ApiResponse(responseCode = "200", description = "Imagen"),
                        @ApiResponse(responseCode = "404", description = "Foto no encontrada")
        })
        @GetMapping("/photos/{photoId}")
        public ResponseEntity<Resource> getPhoto(@PathVariable("photoId") UUID photoId) {
                return venueService.getPhotoResource(photoId);
        }

        // ─────────────────────────────────────────────
        // DELETE /venues/photos/{photoId}
        // ─────────────────────────────────────────────

        /**
         * Elimina una imagen de venue (registro de BD y archivo físico).
         *
         * <p>
         * Requiere autenticación. Solo el dueño del venue puede borrar sus fotos.
         *
         * @param photoId UUID del registro a eliminar.
         */
        @Operation(summary = "Eliminar foto del venue", description = "Elimina físicamente la foto del servidor y su registro. Solo permitido para el host dueño de la sede.", security = @SecurityRequirement(name = "bearerAuth"), responses = {
                        @ApiResponse(responseCode = "200", description = "Foto eliminada", content = @Content(schema = @Schema(implementation = MessageResponse.class))),
                        @ApiResponse(responseCode = "403", description = "No es el propietario"),
                        @ApiResponse(responseCode = "404", description = "Foto no encontrada")
        })
        @DeleteMapping("/photos/{photoId}")
        public ResponseEntity<MessageResponse> deletePhoto(
                        @PathVariable("photoId") UUID photoId,
                        @AuthenticationPrincipal UUID userId) {
                return ResponseEntity.ok(venueService.deletePhoto(userId, photoId));
        }

        @Operation(summary = "Reporte de reservas por cancha", description = "Retorna una lista de todas las canchas con la cantidad de reservas y el nombre de su dueño.")
        @GetMapping("/report/bookings")
        public ResponseEntity<List<VenueBookingReportDto>> getVenuesBookingReport() {
                return ResponseEntity.ok(venueService.getVenuesBookingReport());
        }

        @Operation(summary = "Estadísticas de salones y propietarios", description = "Retorna el número total de salones y el número total de propietarios únicos.")
        @GetMapping("/report/stats")
        public ResponseEntity<VenueStatsResponseDto> getVenueStats() {
                return ResponseEntity.ok(venueService.getVenueStats());
        }

        @Operation(summary = "Reporte detallado de salones", description = "Retorna una lista detallada de salones con información del dueño, reservas, ingresos, comisiones, ganancias y ubigeo.")
        @GetMapping("/report/detailed")
        public ResponseEntity<List<VenueDetailedReportDto>> getVenuesDetailedReport() {
                return ResponseEntity.ok(venueService.getVenuesDetailedReport());
        }

        @Operation(summary = "Obtener salones de un host", description = "Retorna el listado de salones de un host específico con su respectiva información de calificación, reservas, ingresos y estado.")
        @GetMapping("/host/{idhost}")
        @PreAuthorize("hasAnyRole('ROLE_CLIENT', 'ROLE_ADMIN')")
        public ResponseEntity<List<HostVenueResponseDto>> getHostVenues(@PathVariable("idhost") UUID idhost) {
                return ResponseEntity.ok(venueService.getHostVenues(idhost));
        }
}
