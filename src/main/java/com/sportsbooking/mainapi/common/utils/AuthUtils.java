package com.sportsbooking.mainapi.common.utils;

import com.sportsbooking.mainapi.common.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;

import java.util.UUID;

/**
 * Utilidades para extracción segura de datos del contexto de seguridad.
 *
 * <p>Centraliza la validación del {@link Authentication} para evitar
 * {@link NullPointerException} cuando el token JWT expira y Spring Security
 * establece un contexto anónimo.
 *
 * <p>Uso:
 * <pre>{@code
 * UUID userId = AuthUtils.extractUserId(authentication);
 * }</pre>
 */
public final class AuthUtils {

    private AuthUtils() {
        // Utility class — no instantiation
    }

    /**
     * Extrae el UUID del usuario desde el {@link Authentication}.
     *
     * <p>Lanza {@link UnauthorizedException} si el objeto es null (token expirado
     * o ausente), lo que genera un HTTP 401 limpio vía
     * {@link com.sportsbooking.mainapi.common.GlobalExceptionHandler}.
     *
     * @param authentication contexto de seguridad inyectado por Spring.
     * @return UUID del usuario autenticado.
     * @throws UnauthorizedException si authentication es null.
     */
    public static UUID extractUserId(Authentication authentication) {
        if (authentication == null) {
            throw new UnauthorizedException(
                    "No autorizado. Proporciona un token JWT válido en el header Authorization."
            );
        }
        return UUID.fromString(authentication.getName());
    }

    /**
     * Extrae el UUID del usuario desde el principal del {@link Authentication}.
     *
     * <p>Variante para controllers que almacenan el UUID directamente como principal
     * (ej: {@code AuthController}).
     *
     * @param authentication contexto de seguridad inyectado por Spring.
     * @return UUID del usuario autenticado.
     * @throws UnauthorizedException si authentication es null.
     */
    public static UUID extractUserIdFromPrincipal(Authentication authentication) {
        if (authentication == null) {
            throw new UnauthorizedException(
                    "No autorizado. Proporciona un token JWT válido en el header Authorization."
            );
        }
        return (UUID) authentication.getPrincipal();
    }
}
