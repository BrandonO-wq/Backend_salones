package com.sportsbooking.mainapi.service;

import com.sportsbooking.mainapi.dto.auth.request.ForgotPasswordRequest;
import com.sportsbooking.mainapi.dto.auth.request.LoginRequest;
import com.sportsbooking.mainapi.dto.auth.request.RefreshTokenRequest;
import com.sportsbooking.mainapi.dto.auth.request.RegisterRequest;
import com.sportsbooking.mainapi.dto.auth.request.ResendOtpRequest;
import com.sportsbooking.mainapi.dto.auth.request.ResetPasswordRequest;
import com.sportsbooking.mainapi.dto.auth.request.UpdateFbTokenRequest;
import com.sportsbooking.mainapi.dto.auth.request.VerifyOtpRequest;
import com.sportsbooking.mainapi.dto.auth.request.VerifyRecoveryOtpRequest;
import com.sportsbooking.mainapi.dto.auth.response.MeResponse;
import com.sportsbooking.mainapi.dto.auth.response.RegisterResponse;
import com.sportsbooking.mainapi.dto.auth.response.ResetTokenResponse;
import com.sportsbooking.mainapi.dto.auth.response.TokenResponse;
import com.sportsbooking.mainapi.dto.common.MessageResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

/**
 * Contrato del servicio de autenticación.
 *
 * <p>Define todas las operaciones de autenticación disponibles en la API:
 * registro con OTP, verificación de OTP, login, logout, renovación de tokens
 * y consulta del usuario autenticado.
 */
public interface AuthService {

    /**
     * Registra un nuevo usuario con rol CLIENT en estado INACTIVO y dispara el envío
     * de un código OTP al email del usuario.
     *
     * @param request datos del nuevo usuario (nombre, email, teléfono, contraseña).
     * @return {@link RegisterResponse} con el {@code userId} del usuario recién creado.
     */
    RegisterResponse register(RegisterRequest request);

    /**
     * Verifica el código OTP recibido por email. Si es correcto, activa al usuario
     * (status ACTIVO) y retorna el par de tokens para auto-login.
     *
     * @param request contiene el {@code userId} y el {@code otpCode} a verificar.
     * @return par de tokens {@code accessToken} y {@code refreshToken}.
     */
    TokenResponse verifyOtp(VerifyOtpRequest request);

    /**
     * Reenvía un nuevo código OTP al email del usuario, siempre que el cooldown haya expirado.
     *
     * @param request contiene el {@code userId} del usuario en estado PENDIENTE.
     * @return mensaje de confirmación.
     */
    MessageResponse resendOtp(ResendOtpRequest request);

    /**
     * Solicita un OTP de recuperación de contraseña al email indicado.
     *
     * <p>Por seguridad, siempre retorna un mensaje genérico sin revelar
     * si el email existe o no en el sistema.
     *
     * @param request contiene el {@code email} del usuario.
     * @return mensaje genérico de confirmación.
     */
    MessageResponse forgotPassword(ForgotPasswordRequest request);

    /**
     * Verifica el OTP de recovery. Si es correcto, genera un reset token
     * opaco en Redis (10 min TTL, un solo uso).
     *
     * @param request contiene el {@code userId} y el {@code otpCode}.
     * @return {@link ResetTokenResponse} con el token para {@code POST /auth/reset-password}.
     */
    ResetTokenResponse verifyRecoveryOtp(VerifyRecoveryOtpRequest request);

    /**
     * Cambia la contraseña del usuario usando un reset token válido.
     *
     * @param request contiene el {@code resetToken} y la {@code newPassword}.
     * @return mensaje de confirmación.
     */
    MessageResponse resetPassword(ResetPasswordRequest request);

    /**
     * Autentica un usuario ACTIVO con rol CLIENT y retorna el par de tokens.
     *
     * @param request credenciales del usuario (email y contraseña).
     * @return par de tokens {@code accessToken} y {@code refreshToken}.
     */
    TokenResponse login(LoginRequest request);

    /**
     * Autentica un usuario ACTIVO con rol ADMIN y retorna el par de tokens.
     *
     * <p>Falla con error 403 si el usuario no tiene rol ADMIN.
     *
     * @param request credenciales del administrador (email y contraseña).
     * @return par de tokens {@code accessToken} y {@code refreshToken}.
     */
    TokenResponse adminLogin(LoginRequest request);

    /**
     * Rota el par de tokens: revoca el refresh token actual y genera uno nuevo.
     *
     * @param request contiene el refresh token vigente.
     * @return nuevo par de tokens {@code accessToken} y {@code refreshToken}.
     */
    TokenResponse refreshToken(RefreshTokenRequest request);

    /**
     * Cierra la sesión del usuario: extrae el token del header, lo banea en Redis y revoca
     * todos los refresh tokens activos del usuario en la BD.
     *
     * @param request contiene el token JWT del usuario a desloguear (en el header Authorization).
     * @return mensaje de confirmación de cierre de sesión.
     */
    MessageResponse logout(HttpServletRequest request);

    /**
     * Actualiza el token de Firebase (FCM) del usuario autenticado.
     *
     * <p>El {@code userId} se toma del JWT del usuario ya autenticado,
     * no es enviado en el body por seguridad.
     *
     * @param userId  ID del usuario autenticado (extraído del JWT).
     * @param request contiene el nuevo {@code fbToken}.
     * @return mensaje de confirmación.
     */
    MessageResponse updateFbToken(UUID userId, UpdateFbTokenRequest request);

    /**
     * Retorna la información del usuario autenticado actualmente.
     *
     * @param userId ID del usuario extraído del JWT por el filtro de seguridad.
     * @return datos del usuario: id, nombre, email y roles.
     */
    MeResponse getMe(UUID userId);
}
