package com.sportsbooking.mainapi.jpa.repository;

import com.sportsbooking.mainapi.jpa.entity.ParameterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio JPA para la entidad {@link ParameterEntity}.
 *
 * <p>Para queries complejas usar {@code ParameterMyRepository} (MyBatis).
 */
public interface ParameterRepository extends JpaRepository<ParameterEntity, Integer> {

    /**
     * Busca parámetros activos por parent.
     *
     * @param idParamParent ID del parámetro padre.
     * @param status        estado (1=ACTIVO).
     * @return lista de parámetros hijos activos.
     */
    List<ParameterEntity> findByIdParamParentAndStatus(int idParamParent, int status);
}
