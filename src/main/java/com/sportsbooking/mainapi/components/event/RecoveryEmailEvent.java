package com.sportsbooking.mainapi.components.event;

/**
 * Evento de dominio disparado cuando un usuario solicita recuperación de contraseña
 * y debe recibir un email con su OTP de recovery.
 *
 * <p>Se publica tras la transacción ({@code AFTER_COMMIT}) para garantizar
 * consistencia antes de enviar el email.
 *
 * @param userName  nombre completo del usuario.
 * @param userEmail email del usuario (destinatario del correo OTP).
 * @param otpCode   código OTP generado (4 dígitos numéricos) para incluir en el email.
 */
public record RecoveryEmailEvent(String userName, String userEmail, String otpCode) {
}
