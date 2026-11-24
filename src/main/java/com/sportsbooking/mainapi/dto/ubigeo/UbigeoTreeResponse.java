package com.sportsbooking.mainapi.dto.ubigeo;

import java.util.List;

/**
 * Respuesta jerárquica completa del árbol de ubigeos del Perú.
 *
 * <p>Estructura: Departamento → Provincia → Distrito.
 * Cada nivel contiene su código ubigeo (2/4/6 dígitos) y nombre.
 *
 * @param departamentos lista de departamentos con sus provincias y distritos.
 */
public record UbigeoTreeResponse(List<Departamento> departamentos) {

    /**
     * Departamento (nivel 1, código 2 dígitos).
     *
     * @param code   código ubigeo de 2 dígitos (ej: "15" = Lima).
     * @param name   nombre del departamento.
     * @param provincias lista de provincias del departamento.
     */
    public record Departamento(
            String code,
            String name,
            List<Provincia> provincias
    ) {}

    /**
     * Provincia (nivel 2, código 4 dígitos).
     *
     * @param code   código ubigeo de 4 dígitos (ej: "1501" = Lima).
     * @param name   nombre de la provincia.
     * @param distritos lista de distritos de la provincia.
     */
    public record Provincia(
            String code,
            String name,
            List<Distrito> distritos
    ) {}

    /**
     * Distrito (nivel 3, código 6 dígitos).
     *
     * @param code código ubigeo de 6 dígitos (ej: "150101" = Lima).
     * @param name nombre del distrito.
     * @param latitude latitud del distrito.
     * @param longitude longitud del distrito.
     */
    public record Distrito(
            String code,
            String name,
            Double latitude,
            Double longitude
    ) {}
}
