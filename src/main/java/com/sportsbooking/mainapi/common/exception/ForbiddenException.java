package com.sportsbooking.mainapi.common.exception;

/**
 * Excepción lanzada cuando un usuario intenta acceder a un recurso
 * para el cual no tiene permisos suficientes.
 *
 * <p>Corresponde a un error HTTP 403 (Forbidden).
 * Ejemplo: un usuario CLIENT intenta usar el endpoint de admin login.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
