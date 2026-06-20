package com.sportsbooking.mainapi.components;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Componente que detecta el perfil activo de Spring para adaptar
 * el comportamiento de la aplicación por entorno.
 *
 * <p>Uso principal: en el {@code SecurityConfig} para deshabilitar
 * la autenticación JWT cuando se trabaja en entorno local.
 *
 * <p>Perfiles disponibles: {@code local}, {@code dev}, {@code qa}, {@code prod}.
 *
 * <p>Ejemplo:
 * <pre>{@code
 * if (profileChecker.isLocalProfile()) {
 *     // permitir todo sin JWT
 * }
 * }</pre>
 */
@Component
@RequiredArgsConstructor
public class ProfileCheckerComponent {

    private final Environment env;

    /**
     * Verifica si el perfil indicado está activo.
     *
     * @param inProfile nombre del perfil a verificar.
     * @return {@code true} si el perfil está activo.
     */
    private boolean isProfile(String inProfile) {
        String[] activeProfiles = env.getActiveProfiles();
        for (String profile : activeProfiles) {
            if (inProfile.equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    /** @return {@code true} si el perfil activo es {@code local}. */
    public boolean isLocalProfile() { return isProfile("local"); }

    /** @return {@code true} si el perfil activo es {@code dev}. */
    public boolean isDevProfile() { return isProfile("dev"); }

    /** @return {@code true} si el perfil activo es {@code qa}. */
    public boolean isQaProfile() { return isProfile("qa"); }

    /** @return {@code true} si el perfil activo es {@code prod}. */
    public boolean isProdProfile() { return isProfile("prod"); }
}
