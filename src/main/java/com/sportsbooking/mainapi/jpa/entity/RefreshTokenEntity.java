package com.sportsbooking.mainapi.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad que representa un refresh token almacenado en la BD.
 *
 * <p>Mapeada a la tabla {@code refresh_tokens}.
 * No se guarda el token en texto plano: se almacena su hash SHA-256 ({@code token_hash})
 * para proteger el token ante un acceso no autorizado a la base de datos.
 *
 * <p>Un token está activo cuando {@code revokedAt} es {@code null}.
 * Un token está revocado cuando {@code revokedAt} tiene una fecha.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {

    /** Identificador único del refresh token. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** ID del usuario propietario del token (FK a {@code users.id}). */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Hash SHA-256 del refresh token.
     * Se usa para buscar el token en BD sin exponer el valor real.
     */
    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    /** Fecha y hora en que el token expirará. */
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    /**
     * Fecha y hora en que el token fue revocado.
     * Valor {@code null} indica que el token sigue activo.
     */
    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    /** Fecha y hora de creación del registro. */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // ─────────────────────────────────────────────
    // Métodos auxiliares de estado
    // ─────────────────────────────────────────────

    /**
     * Indica si el token ya expiró según la fecha actual.
     *
     * @return {@code true} si {@code expiresAt} es anterior al momento actual.
     */
    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(expiresAt);
    }

    /**
     * Indica si el token fue revocado explícitamente.
     *
     * @return {@code true} si {@code revokedAt} tiene valor.
     */
    public boolean isRevoked() {
        return revokedAt != null;
    }

    /**
     * Indica si el token está válido (no expirado y no revocado).
     *
     * @return {@code true} si el token puede usarse.
     */
    public boolean isValid() {
        return !isExpired() && !isRevoked();
    }
}
