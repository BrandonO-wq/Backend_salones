package com.sportsbooking.mainapi.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Set;

/**
 * Configuración de Redis para la aplicación.
 *
 * <p>Provee el bean {@link StringRedisTemplate} que se usa para almacenar
 * los access tokens revocados en la ban list (invalidación de sesiones en logout).
 *
 * <p>La conexión a Redis se configura automáticamente con las propiedades
 * definidas en {@code application.yml} bajo {@code spring.data.redis}.
 */
@Slf4j
@Configuration
public class RedisConfig {

    /**
     * Bean de {@link StringRedisTemplate} para manejo de claves y valores String en Redis.
     *
     * <p>Permite personalizar la configuración de serialización o comportamiento
     * en futuras versiones sin afectar el resto de la aplicación.
     *
     * @param connectionFactory fábrica de conexiones Redis autoconfigurada por Spring Boot.
     * @return instancia configurada de {@link StringRedisTemplate}.
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    /**
     * Limpia la caché de Redis específica de la aplicación al arrancar.
     * <p>Busca todas las claves con el prefijo definido en {@link RedisProperties} y las elimina.
     * Esto evita problemas de datos obsoletos tras reiniciar la API (ej: parámetros cacheados).
     */
    @Bean
    public ApplicationRunner clearRedisCacheOnStartup(StringRedisTemplate redisTemplate, RedisProperties redisProperties) {
        return args -> {
            String prefix = redisProperties.getKeyPrefix();
            if (prefix == null || prefix.trim().isEmpty()) {
                log.warn("[Redis] El prefijo de claves (app.redis.key-prefix) está vacío, por seguridad NO se vaciará toda la DB.");
                return;
            }

            String pattern = prefix + "*";
            Set<String> keys = redisTemplate.keys(pattern);

            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("[Redis] Caché limpiada exitosamente al arrancar: se eliminaron {} claves con el prefijo '{}'", keys.size(), prefix);
            } else {
                log.info("[Redis] Caché limpia al arrancar: no se encontraron claves con el prefijo '{}'", prefix);
            }
        };
    }
}
