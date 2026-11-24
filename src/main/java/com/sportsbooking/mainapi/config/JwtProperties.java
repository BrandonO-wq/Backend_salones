package com.sportsbooking.mainapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propiedades de configuración para JWT leídas desde el YAML.
 *
 * <p>
 * Se mapean desde el prefijo {@code app.jwt} en {@code application.yml}:
 * 
 * <pre>{@code
 * app:
 *   jwt:
 *     secret: mi-secreto-seguro
 *     access-token-expiration: 900000
 *     refresh-token-expiration: 604800000
 * }</pre>
 *
 * <p>
 * Valores por defecto:
 * <ul>
 * <li>{@code accessTokenExpiration} = 900000 ms (15 minutos)</li>
 * <li>{@code refreshTokenExpiration} = 604800000 ms (7 días)</li>
 * </ul>
 */
@Data
// @Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** Clave secreta usada para firmar y verificar los tokens JWT. */
    private String secret;

    /** Tiempo de vida del access token en milisegundos. Por defecto: 15 minutos. */
    private long accessTokenExpiration = 900_000L;

    /** Tiempo de vida del refresh token en milisegundos. Por defecto: 7 días. */
    private long refreshTokenExpiration = 604_800_000L;
}
