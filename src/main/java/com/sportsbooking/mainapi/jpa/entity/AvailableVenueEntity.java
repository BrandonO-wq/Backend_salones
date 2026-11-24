package com.sportsbooking.mainapi.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Entidad que representa un horario disponible de un venue.
 *
 * <p>Mapeada a la tabla {@code available_venues}.
 *
 * <ul>
 *   <li>{@code scheduleType}: 1=DIURNO, 2=NOCTURNO</li>
 *   <li>{@code days}: array de días (1=LUN ... 7=DOM)</li>
 *   <li>{@code status}: 1=ACTIVO, 0=INACTIVO</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "available_venues")
public class AvailableVenueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "venue_id", nullable = false)
    private UUID venueId;

    /** Tipo de horario: 1=DIURNO, 2=NOCTURNO. */
    @Column(name = "schedule_type", nullable = false)
    private Integer scheduleType;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** Días de la semana en que aplica (1=LUN ... 7=DOM). */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "days", columnDefinition = "integer[]", nullable = false)
    @Builder.Default
    private Integer[] days = {};

    /** Estado: 1=ACTIVO, 0=INACTIVO. */
    @Column(name = "status", nullable = false)
    @Builder.Default
    private int status = 1;

    // --- NUEVOS CAMPOS (REGLAS DE RESERVA) ---

    @Column(name = "horas_minimas")
    private Integer minHours;

    @Column(name = "horas_maximas")
    private Integer maxHours;

    @Column(name = "precio_hora_extra")
    private BigDecimal extraHourPrice;

    @Column(name = "dias_anticipacion")
    private Integer minAdvanceDays;

    @Column(name = "politica_cancelacion")
    private String cancellationPolicy;
}
