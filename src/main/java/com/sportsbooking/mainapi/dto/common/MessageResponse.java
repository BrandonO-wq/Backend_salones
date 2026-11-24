package com.sportsbooking.mainapi.dto.common;

/**
 * Respuesta simple con un mensaje de texto.
 *
 * <p>Usada en endpoints que confirman una operación sin retornar datos adicionales,
 * como {@code POST /auth/logout}.
 *
 * <p>Ejemplo:
 * <pre>{@code
 * { "message": "Session closed successfully" }
 * }</pre>
 */
public record MessageResponse(String message) {}
