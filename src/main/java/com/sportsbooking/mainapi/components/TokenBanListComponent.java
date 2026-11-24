package com.sportsbooking.mainapi.components;

import com.sportsbooking.mainapi.config.RedisProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Componente utilitario para ban list de tokens JWT en Redis.
 *
 * <p>No forma parte del dominio de negocio; es una utilidad técnica de seguridad
 * que invalida inmediatamente un access token al hacer logout, sin esperar
 * a que expire de forma natural.
 *
 * <p>Estrategia de almacenamiento:
 * <ul>
 *   <li>Clave Redis: {@code {prefix}banned_token:{token_jwt}}</li>
 *   <li>Valor: {@code "1"} (solo importa la existencia de la clave)</li>
 *   <li>TTL: tiempo restante de vida del token en milisegundos</li>
 * </ul>
 *
 * <p>Cuando el token expira de forma natural, Redis elimina la clave automáticamente,
 * manteniendo la ban list limpia sin intervención manual.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenBanListComponent {

    private static final String BAN_KEY_SUFFIX = "banned_token:";

    private final StringRedisTemplate redisTemplate;
    private final RedisProperties redisProperties;

    /**
     * Agrega un token a la ban list con el tiempo de vida indicado.
     *
     * <p>Si el TTL es menor o igual a cero (token ya expirado), no se almacena.
     *
     * @param token token JWT a banear (el access token completo).
     * @param ttlMs tiempo de vida restante del token en milisegundos.
     */
    public void banToken(String token, long ttlMs) {
        if (ttlMs <= 0) {
            log.debug("Token ya expirado, no es necesario agregarlo a la ban list");
            return;
        }
        String key = redisProperties.getKeyPrefix() + BAN_KEY_SUFFIX + token;
        redisTemplate.opsForValue().set(key, "1", ttlMs, TimeUnit.MILLISECONDS);
        log.info("Token agregado a ban list de Redis. TTL: {} ms", ttlMs);
    }

    /**
     * Verifica si un token está en la ban list.
     *
     * <p>Una clave existente significa que el token fue baneado por logout.
     *
     * @param token token JWT a verificar.
     * @return {@code true} si el token fue baneado.
     */
    public boolean isTokenBanned(String token) {
        String key = redisProperties.getKeyPrefix() + BAN_KEY_SUFFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
