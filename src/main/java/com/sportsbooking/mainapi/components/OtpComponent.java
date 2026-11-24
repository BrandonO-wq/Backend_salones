package com.sportsbooking.mainapi.components;

import com.sportsbooking.mainapi.config.OtpProperties;
import com.sportsbooking.mainapi.config.RedisProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

/**
 * Componente utilitario para gestión de códigos OTP (One-Time Password) en Redis.
 *
 * <p>No forma parte del dominio de negocio; es una utilidad técnica de soporte
 * al flujo de verificación de cuenta vía email.
 *
 * <p>Keys Redis utilizadas (con prefijo {@code app.redis.key-prefix}):
 * <ul>
 *   <li>{@code {prefix}otp:{userId}} – código OTP activo con TTL configurable ({@code app.otp.expiration-minutes}).</li>
 *   <li>{@code {prefix}otp_cooldown:{userId}} – indicador de cooldown con TTL configurable ({@code app.otp.cooldown-minutes}).</li>
 * </ul>
 *
 * <p>El código OTP es un número de 4 dígitos ({@code 0000}–{@code 9999}) generado con
 * {@link SecureRandom} para evitar predictibilidad.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OtpComponent {

    private final StringRedisTemplate redisTemplate;
    private final OtpProperties otpProperties;
    private final RedisProperties redisProperties;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // ─────────────────────────────────────────────
    // Keys Redis
    // ─────────────────────────────────────────────

    private String otpKey(UUID userId) {
        return redisProperties.getKeyPrefix() + "otp:" + userId;
    }

    private String cooldownKey(UUID userId) {
        return redisProperties.getKeyPrefix() + "otp_cooldown:" + userId;
    }

    // ─────────────────────────────────────────────
    // API pública
    // ─────────────────────────────────────────────

    /**
     * Genera un código OTP de 4 dígitos, lo almacena en Redis con TTL y registra
     * la key de cooldown para evitar reenvíos inmediatos.
     *
     * @param userId ID del usuario para quien se genera el OTP.
     * @return el código OTP generado (texto plano, para enviarse por email).
     */
    public String generateAndStoreOtp(UUID userId) {
        String otp = String.format("%04d", SECURE_RANDOM.nextInt(10_000));

        Duration otpTtl      = Duration.ofMinutes(otpProperties.getExpirationMinutes());
        Duration cooldownTtl = Duration.ofMinutes(otpProperties.getCooldownMinutes());

        redisTemplate.opsForValue().set(otpKey(userId), otp, otpTtl);
        redisTemplate.opsForValue().set(cooldownKey(userId), "1", cooldownTtl);

        log.debug("OTP generado y almacenado en Redis para userId: {}", userId);
        return otp;
    }

    /**
     * Valida el código OTP recibido contra el almacenado en Redis.
     *
     * <p>Si la validación es exitosa, elimina ambas keys para evitar reutilización.
     * Si el código ya expiró en Redis, retorna {@code false} directamente.
     *
     * @param userId  ID del usuario.
     * @param otpCode código OTP a validar.
     * @return {@code true} si el código es correcto y está vigente.
     */
    public boolean validateOtp(UUID userId, String otpCode) {
        String stored = redisTemplate.opsForValue().get(otpKey(userId));

        if (stored == null) {
            log.warn("OTP expirado o no encontrado para userId: {}", userId);
            return false;
        }

        if (!stored.equals(otpCode)) {
            log.warn("OTP incorrecto para userId: {}", userId);
            return false;
        }

        deleteOtp(userId);
        log.info("OTP validado correctamente para userId: {}", userId);
        return true;
    }

    /**
     * Verifica si el usuario aún está dentro del período de cooldown
     * (no puede volver a solicitar un OTP hasta que expire).
     *
     * @param userId ID del usuario.
     * @return {@code true} si el cooldown está activo.
     */
    public boolean isCooldownActive(UUID userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey(userId)));
    }

    /**
     * Elimina manualmente las keys OTP y cooldown de Redis para el usuario indicado.
     *
     * <p>Útil para limpieza en caso de re-registro de un usuario INACTIVO.
     *
     * @param userId ID del usuario.
     */
    public void deleteOtp(UUID userId) {
        redisTemplate.delete(otpKey(userId));
        redisTemplate.delete(cooldownKey(userId));
        log.debug("Keys OTP eliminadas de Redis para userId: {}", userId);
    }

    // ─────────────────────────────────────────────
    // Rate Limiting por email
    // ─────────────────────────────────────────────

    private String rateLimitKey(String email) {
        return redisProperties.getKeyPrefix() + "otp_rate:" + email.toLowerCase();
    }

    /**
     * Verifica y registra una solicitud OTP para el email dado.
     *
     * <p>Usa un contador Redis con TTL de {@code maxRequestsWindowHours}.
     * Si el número de solicitudes supera {@code maxRequests}, lanza HTTP 429
     * con un mensaje indicando cuántas horas esperar.
     *
     * @param email email del usuario solicitante.
     * @throws ResponseStatusException 429 si se excede el límite.
     */
    public void checkAndIncrementOtpRate(String email) {
        String key = rateLimitKey(email);
        int maxRequests = otpProperties.getMaxRequests();
        int windowHours = otpProperties.getMaxRequestsWindowHours();

        Long current = redisTemplate.opsForValue().increment(key);
        if (current == null) current = 1L;

        // Setear TTL solo en el primer incremento
        if (current == 1L) {
            redisTemplate.expire(key, Duration.ofHours(windowHours));
        }

        if (current > maxRequests) {
            Long ttlSeconds = redisTemplate.getExpire(key);
            String waitTime = formatWaitTime(ttlSeconds);
            log.warn("Rate limit OTP excedido para email: {} (count={})", email, current);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Has superado el límite de solicitudes. Intenta de nuevo en " + waitTime + ".");
        }

        log.debug("OTP request count para {}: {}/{}", email, current, maxRequests);
    }

    /**
     * Formatea los segundos restantes de TTL en un texto legible (ej: "3 horas", "45 minutos").
     */
    private String formatWaitTime(Long ttlSeconds) {
        if (ttlSeconds == null || ttlSeconds <= 0) {
            return otpProperties.getMaxRequestsWindowHours() + " horas";
        }
        long hours = ttlSeconds / 3600;
        long minutes = (ttlSeconds % 3600) / 60;
        if (hours > 0) {
            return hours + (hours == 1 ? " hora" : " horas");
        }
        return minutes + (minutes == 1 ? " minuto" : " minutos");
    }

    // ─────────────────────────────────────────────
    // Reset Token (password recovery)
    // ─────────────────────────────────────────────

    private String resetTokenKey(String token) {
        return redisProperties.getKeyPrefix() + "reset:" + token;
    }

    /**
     * Genera un token opaco (UUID) para el flujo de reset de contraseña.
     *
     * <p>Almacena la relación {@code resetToken → userId} en Redis con
     * TTL igual a {@code expiration-minutes} (mismo que el OTP).
     *
     * @param userId ID del usuario que verificó su OTP de recovery.
     * @return token opaco (UUID string) para usar en {@code POST /auth/reset-password}.
     */
    public String generateResetToken(UUID userId) {
        String token = UUID.randomUUID().toString();
        Duration ttl = Duration.ofMinutes(otpProperties.getExpirationMinutes());
        redisTemplate.opsForValue().set(resetTokenKey(token), userId.toString(), ttl);
        log.debug("Reset token generado para userId: {}", userId);
        return token;
    }

    /**
     * Valida y consume un reset token de forma atómica (GET + DELETE).
     *
     * <p>Si el token es válido, retorna el userId asociado y lo elimina de Redis
     * para garantizar un solo uso. Si es inválido o expirado, retorna {@code null}.
     *
     * @param token reset token a validar.
     * @return UUID del usuario, o {@code null} si el token no existe o expiró.
     */
    public UUID validateAndConsumeResetToken(String token) {
        String key = resetTokenKey(token);
        String userId = redisTemplate.opsForValue().get(key);
        if (userId == null) {
            log.warn("Reset token inválido o expirado: {}", token);
            return null;
        }
        redisTemplate.delete(key);
        log.info("Reset token consumido para userId: {}", userId);
        return UUID.fromString(userId);
    }
}
