package com.sportsbooking.mainapi.dto.auth.response;

/**
 * Respuesta tras verificar exitosamente el OTP de recovery.
 *
 * <p>Contiene un token opaco (UUID) de un solo uso con TTL de 10 min en Redis,
 * que el cliente usará en {@code POST /auth/reset-password}.
 *
 * @param resetToken token opaco para autorizar el cambio de contraseña.
 */
public record ResetTokenResponse(String resetToken) {}
