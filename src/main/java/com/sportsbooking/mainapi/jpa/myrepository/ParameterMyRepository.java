package com.sportsbooking.mainapi.jpa.myrepository;

import com.sportsbooking.mainapi.dto.parameter.ParameterDto;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio MyBatis para la tabla {@code parameter}.
 *
 * <p>Los mappers XML se encuentran en {@code sqlmapPg/mapper/parameter-mapper.xml}.
 */
@Repository
public interface ParameterMyRepository {

    /**
     * Retorna todos los parámetros hijos activos de un parent.
     *
     * @param idParamParent ID del parámetro padre.
     * @return lista de parámetros hijos.
     */
    List<ParameterDto> getChildrenByParent(@Param("idParamParent") Integer idParamParent);

    /**
     * Retorna la descripción de un parámetro por su ID.
     *
     * @param idParam ID del parámetro.
     * @return descripción, o null si no existe.
     */
    String getDescriptionById(@Param("idParam") Integer idParam);
}
