package com.sportsbooking.mainapi.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Componente que se ejecuta automáticamente al iniciar la aplicación.
 * Evalúa si la propiedad "app.redis.clear-on-startup" es verdadera,
 * y de ser así, borra todas las keys que coincidan con el prefijo
 * para asegurar un caché fresco en modo desarrollo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStartupCacheClearer {

    private final StringRedisTemplate redisTemplate;
    private final RedisProperties redisProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (redisProperties.isClearOnStartup()) {
            String pattern = redisProperties.getKeyPrefix() + "*";
            log.info("[Redis] Iniciando limpieza de cache de arranque (clear-on-startup=true). Pattern: {}", pattern);
            
            try {
                Set<String> keys = redisTemplate.keys(pattern);
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    log.info("[Redis] Se han limpiado {} keys del cache exitosamente.", keys.size());
                } else {
                    log.info("[Redis] El cache estaba vacío, no hubo keys que limpiar.");
                }
            } catch (Exception e) {
                log.error("[Redis] Error al intentar limpiar el cache en el arranque: {}", e.getMessage());
            }
        } else {
            log.debug("[Redis] clear-on-startup está deshabilitado. Se mantiene el caché actual.");
        }
    }
}
