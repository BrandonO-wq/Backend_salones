package com.sportsbooking.mainapi.enums;

/**
 * Roles disponibles en la plataforma CanchaGo.
 *
 * <p>Uso:
 * <ul>
 *   <li>{@code CLIENT} – usuario regular que puede reservar canchas y también gestionar venues.</li>
 *   <li>{@code ADMIN}  – administrador de la plataforma con acceso total.</li>
 * </ul>
 *
 * <p>Los roles se almacenan en la tabla {@code users} como texto separado por comas,
 * por ejemplo: {@code "CLIENT"}.
 */
public enum RoleEnum {
    CLIENT,
    ADMIN
}
