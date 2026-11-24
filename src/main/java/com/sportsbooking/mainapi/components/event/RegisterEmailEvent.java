package com.sportsbooking.mainapi.components.event;

/**
 * Evento de dominio disparado cuando un usuario se registra y debe recibir un email con su OTP.
 *
 * <p>Se publica tras la transacción ({@code AFTER_COMMIT}) para garantizar que el usuario
 * ya fue persistido en BD antes de enviar el email.
 *
 * @param userName  nombre completo del usuario recién registrado.
 * @param userEmail email del usuario (destinatario del correo OTP).
 * @param otpCode   código OTP generado (4 dígitos numéricos) para incluir en el email.
 */
public record RegisterEmailEvent(String userName, String userEmail, String otpCode) {
}
