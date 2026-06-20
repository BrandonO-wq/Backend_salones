package com.sportsbooking.mainapi.jpa.repository;

import com.sportsbooking.mainapi.jpa.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad {@link UserEntity}.
 *
 * <p>Provee acceso a la tabla {@code users} con métodos de búsqueda
 * adicionales a los heredados de {@link JpaRepository}.
 */
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    /**
     * Busca un usuario por su email (sin filtrar por status).
     *
     * @param email email a buscar.
     * @return {@link Optional} con el usuario si existe, vacío si no.
     */
    Optional<UserEntity> findByEmail(String email);

    /**
     * Busca un usuario por email y status.
     *
     * <p>Útil para login (buscar solo ACTIVO) y para verificar si ya
     * existe un usuario pendiente con ese email.
     *
     * @param email  email a buscar.
     * @param status valor numérico del estado {@link com.sportsbooking.mainapi.enums.UserStatusEnum}.
     * @return {@link Optional} con el usuario encontrado, o vacío si no existe.
     */
    Optional<UserEntity> findByEmailAndStatus(String email, int status);

    /**
     * Verifica si existe un usuario activo con el email indicado.
     *
     * <p>Para el registro solo bloqueamos si existe un usuario con status ACTIVO (1).
     * Un usuario INACTIVO puede re-registrarse con el mismo email.
     *
     * @param email  email a verificar.
     * @param status valor numérico del estado a filtrar.
     * @return {@code true} si existe un usuario con ese email y estado.
     */
    boolean existsByEmailAndStatus(String email, int status);

    /**
     * Busca un usuario por su token FCM.
     *
     * <p>Usado por {@code PushNotificationServiceImpl} para limpiar tokens
     * inválidos cuando FCM responde con {@code UNREGISTERED}.
     *
     * @param fbToken token FCM a buscar.
     * @return {@link Optional} con el usuario si existe, vacío si no.
     */
    Optional<UserEntity> findByFbToken(String fbToken);
}
