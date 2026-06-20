package com.sportsbooking.mainapi.dto.auth.response;

/**
 * Respuesta generada tras un proceso de autenticación exitoso.
 *
 * <p>Usada en los endpoints:
 * <ul>
 *   <li>{@code POST /auth/register}</li>
 *   <li>{@code POST /auth/login}</li>
 *   <li>{@code POST /auth/admin/login}</li>
 *   <li>{@code POST /auth/refresh}</li>
 * </ul>
 *
 * <p>El cliente debe almacenar el {@code accessToken} en memoria (nunca en localStorage)
 * y el {@code refreshToken} de manera segura para renovar la sesión.
 *
 * <p>Ejemplo de respuesta:
 * <pre>{@code
 * {
 *   "accessToken":  "eyJhbGciOiJIUzI1NiIs...",
 *   "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
 * }
 * }</pre>
 */
public record TokenResponse(

        /** JWT de acceso, válido por 15 minutos. */
        String accessToken,

        /** JWT de renovación, válido por 7 días. Se rota con cada uso. */
        String refreshToken
) {}
