package com.sportsbooking.mainapi.common.exception;

/**
 * Excepción lanzada cuando se viola una regla de negocio.
 *
 * <p>Corresponde a un error HTTP 409 (Conflict).
 * Ejemplo: intentar registrar un email que ya está en uso,
 * o usar un refresh token ya revocado.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
