package com.sportsbooking.mainapi.common.exception;

/**
 * Excepción lanzada cuando las credenciales proporcionadas son incorrectas.
 *
 * <p>Corresponde a un error de autenticación fallida (HTTP 401).
 * Ejemplo: email no encontrado o contraseña incorrecta.
 */
public class BadCredentialsException extends RuntimeException {

    public BadCredentialsException(String message) {
        super(message);
    }
}
