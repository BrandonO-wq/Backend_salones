package com.sportsbooking.mainapi.jpa.repository;

import com.sportsbooking.mainapi.jpa.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad {@link RefreshTokenEntity}.
 *
 * <p>Provee acceso a la tabla {@code refresh_tokens} con métodos
 * de búsqueda y revocación de tokens.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    /**
     * Busca un refresh token activo (no revocado) por su hash.
     *
     * @param tokenHash hash SHA-256 del token recibido.
     * @return un {@link Optional} con el token encontrado, o vacío si no existe o está revocado.
     */
    Optional<RefreshTokenEntity> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    /**
     * Revoca todos los refresh tokens activos de un usuario.
     * Se usa al hacer logout para invalida todas las sesiones del usuario.
     *
     * @param userId ID del usuario cuyos tokens se deben revocar.
     */
    @Modifying
    @Query("UPDATE RefreshTokenEntity r SET r.revokedAt = CURRENT_TIMESTAMP " +
           "WHERE r.userId = :userId AND r.revokedAt IS NULL")
    void revokeAllActiveByUserId(@Param("userId") UUID userId);
}
