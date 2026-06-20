package com.sportsbooking.mainapi.jpa.repository;

import com.sportsbooking.mainapi.jpa.entity.VenueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import com.sportsbooking.mainapi.jpa.entity.enums.VenueStatus;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad {@link VenueEntity}.
 *
 * <p>CRUD simple. Para queries geoespaciales y de búsqueda usar
 * {@code VenueMyRepository} (MyBatis).
 */
public interface VenueRepository extends JpaRepository<VenueEntity, UUID> {

    /**
     * Retorna los venues por su estado según el host indicado.
     *
     * @param hostUserId ID del usuario HOST.
     * @param status     Estado del venue.
     * @return lista de venues.
     */
    List<VenueEntity> findByHostUserIdAndStatus(UUID hostUserId, VenueStatus status);
}
