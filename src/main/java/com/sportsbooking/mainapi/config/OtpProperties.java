package com.sportsbooking.mainapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de configuración del OTP (One-Time Password).
 *
 * <p>Cargadas desde el YAML bajo {@code app.otp}.
 *
 * <p>Ejemplo en YAML:
 * <pre>{@code
 * app:
 *   otp:
 *     expiration-minutes: 5
 *     cooldown-minutes: 2
 * }</pre>
 */
@Data
@ConfigurationProperties(prefix = "app.otp")
public class OtpProperties {

    /**
     * Tiempo de validez del código OTP en minutos.
     * <p>Default: {@code 5}
     */
    private int expirationMinutes = 5;

    /**
     * Tiempo mínimo entre reenvíos de OTP en minutos.
     * <p>Default: {@code 2}
     */
    private int cooldownMinutes = 2;

    /**
     * Máximo de solicitudes OTP permitidas dentro de la ventana.
     * <p>Default: {@code 3}
     */
    private int maxRequests = 3;

    /**
     * Ventana de tiempo para el rate-limit de OTP en horas.
     * <p>Default: {@code 4}
     */
    private int maxRequestsWindowHours = 4;
}
