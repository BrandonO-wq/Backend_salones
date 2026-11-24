package com.sportsbooking.mainapi.dto.common;

import java.util.List;

/**
 * Estructura estándar para respuestas de error en la API.
 *
 * <p>Todos los errores devueltos por la API siguen este formato para
 * facilitar el manejo uniforme en el cliente.
 *
 * <p>Ejemplo de respuesta de error de validación (HTTP 400):
 * <pre>{@code
 * {
 *   "code":    "VALIDATION_ERROR",
 *   "message": "Los datos enviados no son válidos",
 *   "details": ["El email no tiene un formato válido", "La contraseña es obligatoria"]
 * }
 * }</pre>
 *
 * <p>Ejemplo de error de autenticación (HTTP 401):
 * <pre>{@code
 * {
 *   "code":    "BAD_CREDENTIALS",
 *   "message": "Email o contraseña incorrectos",
 *   "details": null
 * }
 * }</pre>
 *
 * @param code    código identificador del error (ej: {@code "VALIDATION_ERROR"}).
 * @param message descripción legible del error para el cliente.
 * @param details lista opcional con detalles adicionales del error (ej: campos inválidos).
 */
public record ErrorResponse(
        String code,
        String message,
        List<String> details
) {}
