package com.sportsbooking.mainapi.jpa.repository;

import com.sportsbooking.mainapi.dto.venue.response.VenueBookingReportDto;
import com.sportsbooking.mainapi.dto.venue.response.VenueStatsResponseDto;
import com.sportsbooking.mainapi.jpa.entity.VenueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.sportsbooking.mainapi.jpa.entity.enums.VenueStatus;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad {@link VenueEntity}.
 *
 * <p>
 * CRUD simple. Para queries geoespaciales y de búsqueda usar
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

    List<VenueEntity> findByHostUserId(UUID hostUserId);

    long countByHostUserId(UUID hostUserId);

    /**
     * Retorna el total de salones y propietarios únicos,
     * considerando únicamente hosts con status = 1 (ACTIVO).
     *
     * @return {@link VenueStatsResponseDto} con totales filtrados por host activo.
     */
    @Query("SELECT new com.sportsbooking.mainapi.dto.venue.response.VenueStatsResponseDto(" +
            "COUNT(v), COUNT(DISTINCT v.hostUserId)) " +
            "FROM VenueEntity v " +
            "JOIN UserEntity u ON u.id = v.hostUserId " +
            "WHERE u.status = 1")
    VenueStatsResponseDto getVenueStats();
}
