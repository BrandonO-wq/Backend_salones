package com.sportsbooking.mainapi.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.math.BigDecimal;
import com.sportsbooking.mainapi.jpa.entity.enums.VenueStatus;
import com.sportsbooking.mainapi.jpa.entity.converter.VenueStatusConverter;

/**
 * Entidad que representa un venue (cancha o local deportivo).
 *
 * <p>Mapeada a la tabla {@code venues}. Contiene ubicación geoespacial
 * vía PostGIS ({@link Point}) y arrays de PostgreSQL para deportes,
 * superficies y comodidades.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "venues")
public class VenueEntity {

    /** Identificador único del venue. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** ID del usuario HOST propietario del venue. */
    @Column(name = "host_user_id", nullable = false)
    private UUID hostUserId;

    /** Nombre del venue. */
    @Column(name = "name", nullable = false)
    private String name;

    /** Dirección textual del venue. */
    @Column(name = "address", nullable = false)
    private String address;

    /**
     * Ubicación geoespacial (latitud, longitud) almacenada como
     * {@code GEOGRAPHY(Point, 4326)} en PostGIS.
     */
    @Column(name = "location", columnDefinition = "geography(Point,4326)", nullable = false)
    private Point location;

    /** Código de ubigeo (ubicación geográfica peruana). */
    @Column(name = "ubigeo")
    private String ubigeo;

    /** Descripción del venue. */
    @Column(name = "description")
    private String description;

    /** Número de contacto telefónico o WhatsApp. */
    @Column(name = "contact_number")
    private String contactNumber;

    /** Dimensiones del campo (ej: "40x20 m"). */
    @Column(name = "dimensions")
    private String dimensions;

    /** Indica si el venue tiene iluminación nocturna. */
    @Column(name = "is_illuminated", nullable = false)
    @Builder.Default
    private boolean isIlluminated = false;

    /** IDs de deportes disponibles (referencia a parameter). */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "id_sports", columnDefinition = "integer[]", nullable = false)
    @Builder.Default
    private Integer[] idSports = {};

    /** IDs de tipos de superficie (referencia a parameter). */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "id_surfaces", columnDefinition = "integer[]", nullable = false)
    @Builder.Default
    private Integer[] idSurfaces = {};

    /** IDs de comodidades (referencia a parameter). */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "id_comforts", columnDefinition = "integer[]", nullable = false)
    @Builder.Default
    private Integer[] idComforts = {};

    /** Estado del venue (ACTIVO, INACTIVO). */
    @Convert(converter = VenueStatusConverter.class)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private VenueStatus status = VenueStatus.ACTIVE;

    /** Fecha de creación del registro. */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // --- NUEVOS CAMPOS (SALONES DE EVENTOS) ---

    @Column(name = "min_capacity")
    @Builder.Default
    private Integer minCapacity = 0;

    @Column(name = "max_capacity")
    @Builder.Default
    private Integer maxCapacity = 0;

    @Column(name = "banquet_capacity")
    @Builder.Default
    private Integer banquetCapacity = 0;

    @Column(name = "theater_capacity")
    @Builder.Default
    private Integer theaterCapacity = 0;

    @Column(name = "cocktail_capacity")
    @Builder.Default
    private Integer cocktail_capacity = 0;

    @Column(name = "bathrooms_count")
    @Builder.Default
    private Integer bathroomsCount = 0;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "id_amenities", columnDefinition = "integer[]")
    @Builder.Default
    private Integer[] idAmenities = {};

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "id_furniture", columnDefinition = "integer[]")
    @Builder.Default
    private Integer[] idFurniture = {};

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "id_climate", columnDefinition = "integer[]")
    @Builder.Default
    private Integer[] idClimate = {};

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "id_audio_video", columnDefinition = "integer[]")
    @Builder.Default
    private Integer[] idAudioVideo = {};

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "id_lighting", columnDefinition = "integer[]")
    @Builder.Default
    private Integer[] idLighting = {};

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "id_facilities", columnDefinition = "integer[]")
    @Builder.Default
    private Integer[] idFacilities = {};

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "id_accessibility", columnDefinition = "integer[]")
    @Builder.Default
    private Integer[] idAccessibility = {};

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "id_services", columnDefinition = "integer[]")
    @Builder.Default
    private Integer[] idServices = {};

    @Column(name = "base_price")
    @Builder.Default
    private BigDecimal basePrice = BigDecimal.ZERO;

    @Column(name = "pricing_start_time")
    private LocalTime pricingStartTime;

    @Column(name = "pricing_end_time")
    private LocalTime pricingEndTime;
}
