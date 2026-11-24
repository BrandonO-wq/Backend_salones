package com.sportsbooking.mainapi.common.exception;

/**
 * Excepción lanzada cuando un endpoint protegido recibe un request
 * sin autenticación válida (token expirado, ausente o baneado).
 *
 * <p>El {@link com.sportsbooking.mainapi.common.GlobalExceptionHandler}
 * la atrapa y retorna HTTP 401 con formato {@code ErrorResponse}.
 */
public class UnauthorizedException extends RuntimeException {

    /**
     * Crea la excepción con un mensaje descriptivo.
     *
     * @param message detalle del motivo de rechazo.
     */
    public UnauthorizedException(String message) {
        super(message);
    }
}
