package com.sportsbooking.mainapi.components.event;

/**
 * Evento de dominio disparado cuando un usuario solicita la eliminación de su cuenta
 * y debe recibir un email con su OTP para confirmar.
 *
 * @param userName  nombre completo del usuario.
 * @param userEmail email del usuario (destinatario del correo OTP).
 * @param otpCode   código OTP generado (4 dígitos numéricos) para incluir en el email.
 */
public record DeleteAccountEmailEvent(String userName, String userEmail, String otpCode) {
}
