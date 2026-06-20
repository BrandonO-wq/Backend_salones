package com.sportsbooking.mainapi.service;

import com.sportsbooking.mainapi.dto.parameter.ParameterDto;

import java.util.List;

/**
 * Servicio para consulta de parámetros del sistema.
 *
 * <p>Los resultados se cachean en Redis según la categoría padre.
 */
public interface ParameterService {

    /**
     * Obtiene los parámetros hijos activos de una categoría padre.
     *
     * @param parentId ID del parámetro padre (1=Deportes, 2=Superficies, etc.)
     * @return lista de DTOs con los parámetros hijos activos.
     */
    List<ParameterDto> getByParent(int parentId);
}
