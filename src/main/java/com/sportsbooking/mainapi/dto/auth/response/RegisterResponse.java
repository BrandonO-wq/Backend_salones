package com.sportsbooking.mainapi.dto.auth.response;

import java.util.UUID;

/**
 * Response del endpoint {@code POST /auth/register}.
 *
 * <p>Tras el registro el usuario queda en estado INACTIVO y debe verificar su OTP.
 * Por eso solo se retorna el {@code userId}, que el cliente necesita para llamar a
 * {@code POST /auth/verify-otp} y {@code POST /auth/resend-otp}.
 *
 * @param userId ID del usuario recién creado.
 */
public record RegisterResponse(UUID userId) {
}
