package com.sportsbooking.mainapi.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa un parámetro del catálogo genérico del sistema.
 *
 * <p>
 * Mapeada a la tabla {@code parameter}. Estructura jerárquica padre-hijo
 * donde {@code idParamParent} agrupa parámetros por categoría:
 * <ul>
 * <li>1 → Deportes</li>
 * <li>2 → Superficies</li>
 * <li>3 → Comodidades</li>
 * <li>4 → Bancos</li>
 * <li>5 → Billeteras Digitales</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "parameter")
public class ParameterEntity {

    /**
     * Identificador único del parámetro (asignado manualmente, no auto-generado).
     */
    @Id
    @Column(name = "id_param", nullable = false)
    private Integer idParam;

    /** ID del parámetro padre que agrupa esta categoría. */
    @Column(name = "id_param_parent", nullable = false)
    private Integer idParamParent;

    @Column(name = "id_param_child")
    private Integer idParamChild;

    @Column(name = "parent_name", length = 100)
    private String parentName;

    @Column(name = "child_name", length = 100)
    private String childName;

    /** Descripción extendida del parámetro. */
    @Column(name = "description", length = 200)
    private String description;

    /** Ícono asociado (nombre o URL). */
    @Column(name = "icon", length = 100)
    private String icon;

    @Column(name = "icon_type")
    private Integer iconType;

    @Column(name = "row_order")
    private Integer rowOrder;

    @Column(name = "status")
    private Integer status;

    @Column(name = "value")
    private Double value;

}
