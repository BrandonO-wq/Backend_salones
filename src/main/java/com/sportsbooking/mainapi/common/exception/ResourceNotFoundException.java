package com.sportsbooking.mainapi.common.exception;

/**
 * Excepción lanzada cuando no se encuentra un recurso solicitado.
 *
 * <p>Corresponde a un error HTTP 404.
 * Ejemplo: usuario no encontrado por ID.
 *
 * @param resourceName nombre del recurso (ej: "Usuario").
 * @param identifier   valor del identificador buscado (ej: el UUID).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(resourceName + " no encontrado con identificador: " + identifier);
    }
}
