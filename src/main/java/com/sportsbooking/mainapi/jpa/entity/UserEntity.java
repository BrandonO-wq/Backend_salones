package com.sportsbooking.mainapi.jpa.entity;

import com.sportsbooking.mainapi.enums.RoleEnum;
import com.sportsbooking.mainapi.enums.UserStatusEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Entidad que representa un usuario registrado en la plataforma.
 *
 * <p>
 * Mapeada a la tabla {@code users} en PostgreSQL.
 * Los roles se almacenan como un texto separado por comas (ej:
 * {@code "CLIENT"})
 * y se convierten a/desde {@link List} de {@link RoleEnum} mediante métodos
 * auxiliares.
 *
 * <h3>Ciclo de vida del usuario</h3>
 * <ol>
 * <li>Registro → {@code status=INACTIVO}, {@code isVerified=false}</li>
 * <li>Verifica OTP → {@code status=ACTIVO}, {@code isVerified=true}</li>
 * </ol>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class UserEntity {

    /** Identificador único del usuario (UUID generado por la BD). */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Nombre completo del usuario. */
    @Column(name = "name", nullable = false)
    private String name;

    /** Email único del usuario, usado para autenticación. */
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /** Número de teléfono del usuario (opcional). */
    @Column(name = "phone")
    private String phone;

    /**
     * Hash BCrypt de la contraseña del usuario.
     * Nunca se almacena la contraseña en texto plano.
     */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**
     * Roles del usuario almacenados como texto separado por comas.
     * Ejemplo: {@code "CLIENT"}.
     * Por defecto el valor es {@code "CLIENT"}.
     */
    @Column(name = "roles", nullable = false)
    @Builder.Default
    private String roles = RoleEnum.CLIENT.name();

    /**
     * Estado del usuario.
     * <ul>
     * <li>{@code 0} – INACTIVO: recién registrado o dado de baja</li>
     * <li>{@code 1} – ACTIVO: verificado, puede autenticarse</li>
     * </ul>
     * Al registrarse inicia en {@code INACTIVO (0)}.
     */
    @Column(name = "status", nullable = false)
    @Builder.Default
    private int status = UserStatusEnum.INACTIVO.getValue();

    /**
     * Indica si el usuario verificó su cuenta vía OTP.
     * Se establece en {@code true} al completar la verificación OTP exitosamente.
     */
    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private boolean isVerified = false;

    /** Token FCM del dispositivo para notificaciones push. */
    @Column(name = "fb_token")
    private String fbToken;

    /**
     * Fecha y hora del último inicio de sesión exitoso.
     * {@code null} si el usuario nunca ha hecho login (solo se registró).
     */
    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    /** Fecha y hora de creación del registro. */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "ubigeo")
    private String ubigeo;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    // ─────────────────────────────────────────────
    // Métodos auxiliares para manejo de roles
    // ─────────────────────────────────────────────

    /**
     * Retorna el estado del usuario como enum {@link UserStatusEnum}.
     *
     * @return estado del usuario.
     */
    public UserStatusEnum getStatusEnum() {
        return UserStatusEnum.fromValue(this.status);
    }

    /**
     * Establece el estado del usuario usando el enum {@link UserStatusEnum}.
     *
     * @param statusEnum estado a establecer.
     */
    public void setStatusEnum(UserStatusEnum statusEnum) {
        this.status = statusEnum.getValue();
    }

    /**
     * Retorna la lista de roles del usuario como enums.
     *
     * @return lista de {@link RoleEnum}, nunca nula.
     */
    public List<RoleEnum> getRoleList() {
        if (roles == null || roles.isBlank()) {
            return List.of();
        }
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .map(RoleEnum::valueOf)
                .collect(Collectors.toList());
    }

    /**
     * Establece los roles del usuario a partir de una lista de enums.
     *
     * @param roleList lista de {@link RoleEnum} a asignar.
     */
    public void setRoleList(List<RoleEnum> roleList) {
        this.roles = roleList.stream()
                .map(RoleEnum::name)
                .collect(Collectors.joining(","));
    }

    /**
     * Verifica si el usuario tiene un rol específico.
     *
     * @param role rol a verificar.
     * @return {@code true} si el usuario tiene el rol indicado.
     */
    public boolean hasRole(RoleEnum role) {
        return getRoleList().contains(role);
    }
}
