package com.sportsbooking.mainapi.components;

import com.sportsbooking.mainapi.config.JwtProperties;
import com.sportsbooking.mainapi.jpa.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.sportsbooking.mainapi.common.utils.DateUtilities;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Componente encargado de generar y validar tokens JWT.
 *
 * <p>Utiliza la librería jjwt 0.12.x con algoritmo HMAC-SHA256 (HS256).
 *
 * <p>Claims incluidos en el <strong>access token</strong>:
 * <ul>
 *   <li>{@code sub}   – userId del usuario (UUID).</li>
 *   <li>{@code email} – email del usuario (útil para logging/auditoría sin ir a BD).</li>
 *   <li>{@code roles} – lista de roles del usuario (para autorización en endpoints).</li>
 * </ul>
 *
 * <p>Claims incluidos en el <strong>refresh token</strong>:
 * <ul>
 *   <li>{@code sub} – userId del usuario (solo para identificar al usuario).</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProviderComponent {

    private final JwtProperties jwtProperties;

    // ─────────────────────────────────────────────
    // Generación de tokens
    // ─────────────────────────────────────────────

    /**
     * Genera un access token JWT para el usuario indicado.
     *
     * <p>Incluye los claims: {@code sub} (userId), {@code email} y {@code roles}.
     *
     * @param user entidad del usuario autenticado.
     * @return token JWT firmado como String.
     */
    public String generateAccessToken(UserEntity user) {
        List<String> roleNames = user.getRoleList().stream()
                .map(Enum::name)
                .collect(Collectors.toList());

        long nowMillis = DateUtilities.currentLocalDateTime()
                .atZone(DateUtilities.LIMA_ZONE)
                .toInstant()
                .toEpochMilli();

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", roleNames)
                .issuedAt(new Date(nowMillis))
                .expiration(new Date(nowMillis + jwtProperties.getAccessTokenExpiration()))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Genera un refresh token JWT para el usuario indicado.
     *
     * <p>Solo incluye el claim {@code sub} (userId). No contiene roles ni email
     * ya que su único propósito es permitir la renovación del access token.
     *
     * @param userId ID del usuario.
     * @return token JWT firmado como String.
     */
    public String generateRefreshToken(UUID userId) {
        long nowMillis = DateUtilities.currentLocalDateTime()
                .atZone(DateUtilities.LIMA_ZONE)
                .toInstant()
                .toEpochMilli();

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(new Date(nowMillis))
                .expiration(new Date(nowMillis + jwtProperties.getRefreshTokenExpiration()))
                .signWith(getSigningKey())
                .compact();
    }

    // ─────────────────────────────────────────────
    // Validación y extracción de datos
    // ─────────────────────────────────────────────

    /**
     * Valida que el token JWT tenga una firma válida y no esté expirado.
     *
     * @param token token JWT a validar.
     * @return {@code true} si el token es válido; {@code false} si está expirado o es inválido.
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT inválido: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extrae el ID del usuario ({@code sub}) del token JWT.
     *
     * @param token token JWT válido.
     * @return UUID del usuario.
     */
    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    /**
     * Extrae el email del usuario del access token.
     *
     * @param token access token JWT válido.
     * @return email del usuario, o {@code null} si no está en el token.
     */
    public String getEmailFromToken(String token) {
        return parseClaims(token).get("email", String.class);
    }

    /**
     * Extrae la lista de roles del access token.
     *
     * @param token access token JWT válido.
     * @return lista de nombres de roles; lista vacía si no hay roles en el token.
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Object roles = parseClaims(token).get("roles");
        if (roles instanceof List<?>) {
            return (List<String>) roles;
        }
        return List.of();
    }

    /**
     * Retorna la fecha de expiración del token JWT.
     *
     * <p>Útil para calcular el TTL al agregar un token a la ban list de Redis.
     *
     * @param token token JWT válido.
     * @return fecha de expiración del token.
     */
    public Date getExpirationFromToken(String token) {
        return parseClaims(token).getExpiration();
    }

    // ─────────────────────────────────────────────
    // Métodos privados
    // ─────────────────────────────────────────────

    /**
     * Parsea y retorna los claims del token JWT.
     *
     * @param token token JWT a parsear.
     * @return claims del token.
     * @throws JwtException si el token es inválido o está expirado.
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Construye la clave de firma HMAC-SHA256 a partir del secreto configurado.
     *
     * @return clave de firma.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
