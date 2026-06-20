package com.sportsbooking.mainapi.dto.auth.response;

import java.util.List;
import java.util.UUID;

/**
 * Respuesta del endpoint {@code GET /auth/me}.
 *
 * <p>Retorna la información del usuario autenticado extraída del JWT.
 *
 * <p>Ejemplo de respuesta:
 * <pre>{@code
 * {
 *   "id":    "c12f9c8d-9a34-4c55-8c3e-1e234f789000",
 *   "name":  "Juan Perez",
 *   "email": "juan.perez@gmail.com",
 *   "phone": "+51987654321",
 *   "roles": ["CLIENT"]
 * }
 * }</pre>
 */
public record MeResponse(

        /** Identificador único del usuario. */
        UUID id,

        /** Nombre completo del usuario. */
        String name,

        /** Email del usuario. */
        String email,

        /** Teléfono / celular del usuario. */
        String phone,

        /** Código ubigeo del usuario (ej: "020601"). */
        String ubigeo,

        /** Lista de roles asignados al usuario. */
        List<String> roles
) {}
