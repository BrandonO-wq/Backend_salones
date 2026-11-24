package com.sportsbooking.mainapi.enums;

/**
 * Enum que representa los posibles estados de un usuario en la plataforma.
 *
 * <p>El valor numérico es el que se almacena en la columna {@code status} de la tabla {@code users}.
 *
 * <ul>
 *   <li>{@code INACTIVO (0)} – Usuario recién registrado o dado de baja. No puede autenticarse.</li>
 *   <li>{@code ACTIVO (1)} – Usuario verificado y activo. Puede autenticarse.</li>
 * </ul>
 */
public enum UserStatusEnum {

    INACTIVO(0),
    ACTIVO(1);

    private final int value;

    UserStatusEnum(int value) {
        this.value = value;
    }

    /**
     * Retorna el valor numérico del estado, tal como se almacena en BD.
     *
     * @return valor int del estado.
     */
    public int getValue() {
        return value;
    }

    /**
     * Obtiene el enum correspondiente a un valor numérico.
     *
     * @param value valor numérico del estado.
     * @return {@link UserStatusEnum} correspondiente.
     * @throws IllegalArgumentException si el valor no corresponde a ningún estado.
     */
    public static UserStatusEnum fromValue(int value) {
        for (UserStatusEnum status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("No existe UserStatusEnum con value: " + value);
    }
}
