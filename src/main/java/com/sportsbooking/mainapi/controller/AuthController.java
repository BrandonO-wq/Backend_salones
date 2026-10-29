package com.sportsbooking.mainapi.controller;

import com.sportsbooking.mainapi.components.JwtProviderComponent;
import com.sportsbooking.mainapi.dto.auth.request.GoogleLoginRequest;
import com.sportsbooking.mainapi.dto.auth.request.VerifyDeleteRequest;
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
import com.sportsbooking.mainapi.dto.common.ErrorResponse;
import com.sportsbooking.mainapi.dto.common.MessageResponse;
import com.sportsbooking.mainapi.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador REST para el módulo de autenticación.
 *
 * <p>Expone los endpoints:
 * <ul>
 *   <li>{@code POST /auth/register}          – Registro (crea usuario PENDIENTE + envía OTP).</li>
 *   <li>{@code POST /auth/verify-account}     – Verifica OTP y activa al usuario (auto-login).</li>
 *   <li>{@code POST /auth/resend-otp}         – Reenvía OTP (con cooldown de 2 min).</li>
 *   <li>{@code POST /auth/login}              – Login para usuarios ACTIVOS CLIENT.</li>
 *   <li>{@code POST /auth/admin/login}        – Login exclusivo para administradores ACTIVOS.</li>
 *   <li>{@code POST /auth/refresh}            – Renovación de tokens (refresh rotativo).</li>
 *   <li>{@code POST /auth/logout}             – Cierre de sesión (requiere JWT).</li>
 *   <li>{@code POST /auth/updatefbtoken}   – Actualiza el token de Firebase del usuario (requiere JWT).</li>
 *   <li>{@code GET  /auth/me}                – Perfil del usuario autenticado (requiere JWT).</li>
 * </ul>
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints de autenticación: registro, OTP, login, logout y renovación de tokens")
public class AuthController {

    private final AuthService authService;

    // ─────────────────────────────────────────────
    // POST /auth/register
    // ─────────────────────────────────────────────

    /**
     * Registra un nuevo usuario con rol CLIENT en estado PENDIENTE y envía el OTP por email.
     *
     * @param request datos del nuevo usuario.
     * @return HTTP 201 con {@link RegisterResponse} (solo el userId).
     */
    @PostMapping("/register")
    @Operation(
        summary = "Registro de usuario",
        description = "Crea un usuario en estado PENDIENTE y envía un OTP de 4 dígitos al email."
    )
    @ApiResponse(responseCode = "201", description = "Usuario registrado, OTP enviado al email", content = @Content(schema = @Schema(implementation = RegisterResponse.class)))
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "El email ya está registrado y activo", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    // ─────────────────────────────────────────────
    // POST /auth/verify-otp
    // ─────────────────────────────────────────────

    /**
     * Verifica el código OTP recibido por email. Si es correcto, activa al usuario
     * y retorna el par de tokens para auto-login.
     *
     * @param request userId y otpCode a verificar.
     * @return HTTP 200 con {@link TokenResponse} (accessToken + refreshToken).
     */
    @PostMapping("/verify-account")
    @Operation(
        summary = "Verificación de OTP",
        description = "Valida el código OTP, activa el usuario y retorna tokens para auto-login."
    )
    @ApiResponse(responseCode = "200", description = "OTP correcto, usuario activado y tokens generados", content = @Content(schema = @Schema(implementation = TokenResponse.class)))
    @ApiResponse(responseCode = "400", description = "OTP incorrecto o expirado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<TokenResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    // ─────────────────────────────────────────────
    // POST /auth/resend-otp
    // ─────────────────────────────────────────────

    /**
     * Reenvía el OTP al email del usuario. Solo permitido si el cooldown ha expirado.
     *
     * @param request userId del usuario en estado PENDIENTE.
     * @return HTTP 200 con mensaje de confirmación.
     */
    @PostMapping("/resend-otp")
    @Operation(
        summary = "Reenvío de OTP",
        description = "Genera y envía un nuevo OTP. Requiere que hayan pasado al menos 2 minutos desde el último envío."
    )
    @ApiResponse(responseCode = "200", description = "OTP reenviado exitosamente", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado o no está pendiente", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "429", description = "Cooldown activo: debes esperar antes de reenviar", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<MessageResponse> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        return ResponseEntity.ok(authService.resendOtp(request));
    }

    // ─────────────────────────────────────────────
    // POST /auth/forgot-password
    // ─────────────────────────────────────────────

    /**
     * Solicita un OTP de recuperación de contraseña. Respuesta siempre genérica por seguridad.
     *
     * @param request email del usuario.
     * @return HTTP 200 con mensaje genérico.
     */
    @PostMapping("/forgot-password")
    @Operation(
        summary = "Solicitar recuperación de contraseña",
        description = "Envía un OTP al email si el usuario existe. Siempre retorna un mensaje genérico por seguridad."
    )
    @ApiResponse(responseCode = "200", description = "Solicitud procesada", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    // ─────────────────────────────────────────────
    // POST /auth/verify-recovery-otp
    // ─────────────────────────────────────────────

    /**
     * Verifica el OTP de recovery y retorna un resetToken de un solo uso.
     *
     * @param request userId y otpCode.
     * @return HTTP 200 con {@link ResetTokenResponse}.
     */
    @PostMapping("/verify-recovery-otp")
    @Operation(
        summary = "Verificar OTP de recovery",
        description = "Valida el OTP de recuperación. Si es correcto, retorna un reset token para cambiar la contraseña."
    )
    @ApiResponse(responseCode = "200", description = "OTP correcto, reset token generado", content = @Content(schema = @Schema(implementation = ResetTokenResponse.class)))
    @ApiResponse(responseCode = "400", description = "OTP incorrecto o expirado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ResetTokenResponse> verifyRecoveryOtp(@Valid @RequestBody VerifyRecoveryOtpRequest request) {
        return ResponseEntity.ok(authService.verifyRecoveryOtp(request));
    }

    // ─────────────────────────────────────────────
    // POST /auth/reset-password
    // ─────────────────────────────────────────────

    /**
     * Cambia la contraseña del usuario usando un reset token válido.
     *
     * @param request resetToken y newPassword.
     * @return HTTP 200 con mensaje de confirmación.
     */
    @PostMapping("/reset-password")
    @Operation(
        summary = "Restablecer contraseña",
        description = "Cambia la contraseña del usuario usando el reset token obtenido tras verificar el OTP de recovery."
    )
    @ApiResponse(responseCode = "200", description = "Contraseña actualizada", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "400", description = "Reset token inválido o expirado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    // ─────────────────────────────────────────────
    // POST /auth/login
    // ─────────────────────────────────────────────

    /**
     * Autentica un usuario ACTIVO con rol CLIENT.
     *
     * @param request credenciales del usuario.
     * @return HTTP 200 con {@link TokenResponse}.
     */
    @PostMapping("/login")
    @Operation(
        summary = "Login de usuario",
        description = "Autentica a un usuario ACTIVO y retorna access + refresh tokens."
    )
    @ApiResponse(responseCode = "200", description = "Login exitoso", content = @Content(schema = @Schema(implementation = TokenResponse.class)))
    @ApiResponse(responseCode = "401", description = "Credenciales incorrectas o usuario no activo", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ─────────────────────────────────────────────
    // POST /auth/admin/login
    // ─────────────────────────────────────────────

    /**
     * Autentica a un usuario ACTIVO con rol ADMIN.
     *
     * @param request credenciales del administrador.
     * @return HTTP 200 con {@link TokenResponse}.
     */
    @PostMapping("/admin/login")
    @Operation(
        summary = "Login de administrador",
        description = "Autentica a un usuario ACTIVO con rol ADMIN. Falla con 403 si no tiene el rol."
    )
    @ApiResponse(responseCode = "200", description = "Login de admin exitoso", content = @Content(schema = @Schema(implementation = TokenResponse.class)))
    @ApiResponse(responseCode = "401", description = "Credenciales incorrectas", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "El usuario no tiene rol ADMIN", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<TokenResponse> adminLogin(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.adminLogin(request));
    }

    // ─────────────────────────────────────────────
    // POST /auth/refresh
    // ─────────────────────────────────────────────

    /**
     * Renueva el par de tokens usando un refresh token válido (rotación).
     *
     * @param request contiene el refresh token vigente.
     * @return HTTP 200 con nuevo {@link TokenResponse}.
     */
    @PostMapping("/refresh")
    @Operation(
        summary = "Renovación de tokens",
        description = "Rota el par de tokens: revoca el refresh token actual y genera uno nuevo."
    )
    @ApiResponse(responseCode = "200", description = "Tokens renovados exitosamente", content = @Content(schema = @Schema(implementation = TokenResponse.class)))
    @ApiResponse(responseCode = "409", description = "Refresh token inválido, revocado o expirado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    // ─────────────────────────────────────────────
    // POST /auth/logout
    // ─────────────────────────────────────────────

    /**
     * Cierra la sesión del usuario autenticado.
     *
     * @param httpRequest request HTTP para extraer el token del header.
     * @return HTTP 200 con mensaje de confirmación.
     */
    @PostMapping("/logout")
    @Operation(
        summary = "Cierre de sesión",
        description = "Banea el access token en Redis y revoca todos los refresh tokens del usuario.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "Sesión cerrada exitosamente", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "401", description = "Token no proporcionado o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<MessageResponse> logout(HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.logout(httpRequest));
    }

    // ─────────────────────────────────────────────
    // POST /auth/updatefbtoken
    // ─────────────────────────────────────────────

    /**
     * Actualiza el token de Firebase (FCM) del dispositivo del usuario autenticado.
     *
     * <p>El {@code userId} se extrae del JWT (no del body), por lo que solo puede
     * actualizarse el token del usuario que hizo la petición.
     *
     * @param authentication autenticación del usuario actual (inyectada por Spring Security).
     * @param request        body con el nuevo {@code fbToken}.
     * @return HTTP 200 con mensaje de confirmación.
     */
    @PostMapping("/updatefbtoken")
    @Operation(
        summary = "Actualizar token Firebase",
        description = "Actualiza el fb_token del usuario autenticado en la tabla users.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "FB token actualizado", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "401", description = "Token no proporcionado o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<MessageResponse> updateFbToken(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateFbTokenRequest request
    ) {
        return ResponseEntity.ok(authService.updateFbToken(userId, request));
    }

    // ─────────────────────────────────────────────
    // GET /auth/me
    // ─────────────────────────────────────────────

    /**
     * Retorna el perfil del usuario actualmente autenticado.
     *
     * @param authentication información de autenticación del usuario actual (inyectada por Spring Security).
     * @return HTTP 200 con los datos del usuario ({@link MeResponse}).
     */
    @GetMapping("/me")
    @Operation(
        summary = "Perfil del usuario autenticado",
        description = "Retorna id, nombre, email y roles del usuario autenticado.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "Datos del usuario retornados", content = @Content(schema = @Schema(implementation = MeResponse.class)))
    @ApiResponse(responseCode = "401", description = "Token no proporcionado o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<MeResponse> getMe(
            @AuthenticationPrincipal UUID userId
    ) {
        return ResponseEntity.ok(authService.getMe(userId));
    }

    // ─────────────────────────────────────────────
    // POST /auth/google
    // ─────────────────────────────────────────────

    /**
     * Autentica a un usuario usando Google Sign-In (ID Token).
     *
     * @param request contiene el token de Google.
     * @return HTTP 200 con {@link TokenResponse}.
     */
    @PostMapping("/google")
    @Operation(
        summary = "Login con Google",
        description = "Autentica a un usuario usando Google OAuth2 ID Token. Registra al usuario si no existe."
    )
    @ApiResponse(responseCode = "200", description = "Login con Google exitoso", content = @Content(schema = @Schema(implementation = TokenResponse.class)))
    @ApiResponse(responseCode = "400", description = "Token de Google inválido o expirado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<TokenResponse> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.loginWithGoogle(request));
    }

    // ─────────────────────────────────────────────
    // POST /auth/delete
    // ─────────────────────────────────────────────

    /**
     * Inicia la eliminación de la cuenta del usuario enviando un código OTP.
     * Requiere autenticación JWT.
     *
     * @param userId ID del usuario autenticado (extraído del JWT).
     * @return HTTP 200 con mensaje de confirmación de envío.
     */
    @PostMapping("/delete")
    @Operation(
        summary = "Iniciar eliminación de cuenta",
        description = "Envía un código OTP de verificación al correo del usuario para confirmar la eliminación de la cuenta.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "Código OTP enviado", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<MessageResponse> initiateDelete(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(authService.initiateDeleteAccount(userId));
    }

    // ─────────────────────────────────────────────
    // POST /auth/verify-delete
    // ─────────────────────────────────────────────

    /**
     * Confirma y ejecuta la eliminación definitiva de la cuenta.
     * Requiere autenticación JWT.
     *
     * @param userId  ID del usuario autenticado (extraído del JWT).
     * @param request contiene el código OTP.
     * @return HTTP 200 con mensaje de éxito de eliminación.
     */
    @PostMapping("/verify-delete")
    @Operation(
        summary = "Confirmar eliminación de cuenta",
        description = "Valida el código OTP y elimina la cuenta del usuario de manera permanente.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "Cuenta eliminada de manera permanente", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "400", description = "OTP incorrecto o expirado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<MessageResponse> verifyDelete(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody VerifyDeleteRequest request
    ) {
        return ResponseEntity.ok(authService.verifyDeleteAccount(userId, request));
    }

}
