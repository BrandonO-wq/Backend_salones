package com.sportsbooking.mainapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de configuración para las keys de Redis.
 *
 * <p>Cargadas desde el YAML bajo {@code app.redis}.
 *
 * <p>Ejemplo en YAML:
 * <pre>{@code
 * app:
 *   redis:
 *     key-prefix: sportsbooking:
 * }</pre>
 *
 * @see OtpProperties
 */
@Data
@ConfigurationProperties(prefix = "app.redis")
public class RedisProperties {

    /**
     * Prefijo que se antepone a todas las keys de Redis de la aplicación.
     * <p>Default: {@code sportsbooking:}
     */
    private String keyPrefix = "sportsbooking:";

    /**
     * Define si se debe limpiar todo el caché de Redis (que coincida con el prefijo)
     * al momento de levantar el contenedor de Spring Boot. Útil para desarrollo local.
     */
    private boolean clearOnStartup = false;
}
