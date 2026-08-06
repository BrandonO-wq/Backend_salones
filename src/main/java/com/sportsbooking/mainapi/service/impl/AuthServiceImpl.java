package com.sportsbooking.mainapi.service.impl;

import com.sportsbooking.mainapi.common.exception.BadCredentialsException;
import com.sportsbooking.mainapi.common.exception.BusinessException;
import com.sportsbooking.mainapi.common.exception.ForbiddenException;
import com.sportsbooking.mainapi.common.exception.ResourceNotFoundException;
import com.sportsbooking.mainapi.components.JwtProviderComponent;
import com.sportsbooking.mainapi.components.publisher.RegisterEmailPublisher;
import com.sportsbooking.mainapi.components.publisher.RecoveryEmailPublisher;
import com.sportsbooking.mainapi.dto.auth.request.GoogleLoginRequest;
import com.sportsbooking.mainapi.dto.auth.request.VerifyDeleteRequest;
import com.sportsbooking.mainapi.service.GoogleAuthService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.sportsbooking.mainapi.components.publisher.DeleteAccountEmailPublisher;
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
import com.sportsbooking.mainapi.enums.RoleEnum;
import com.sportsbooking.mainapi.enums.UserStatusEnum;
import com.sportsbooking.mainapi.jpa.entity.RefreshTokenEntity;
import com.sportsbooking.mainapi.jpa.entity.UserEntity;
import com.sportsbooking.mainapi.jpa.repository.RefreshTokenRepository;
import com.sportsbooking.mainapi.jpa.repository.UserRepository;
import com.sportsbooking.mainapi.service.AuthService;
import com.sportsbooking.mainapi.components.OtpComponent;
import com.sportsbooking.mainapi.components.TokenBanListComponent;
import com.sportsbooking.mainapi.common.utils.DateUtilities;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de autenticación.
 *
 * <p>
 * Maneja todo el ciclo de vida de la sesión de usuario:
 * registro con OTP, verificación OTP, login, logout, renovación de tokens y
 * consulta del perfil.
 *
 * <p>
 * Estrategia de seguridad:
 * <ul>
 * <li>{@code Access token} — JWT corto (30 min), baneado en Redis al hacer
 * logout.</li>
 * <li>{@code Refresh token} — JWT largo (7 días), hash SHA-256 guardado en BD,
 * revocado en BD al usarlo (rotación) o al hacer logout.</li>
 * <li>{@code OTP} — código 4 dígitos almacenado en Redis con TTL
 * configurable.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProviderComponent jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final TokenBanListComponent tokenBanListComponent;
    private final OtpComponent otpComponent;
    private final RegisterEmailPublisher registerEmailPublisher;
    private final RecoveryEmailPublisher recoveryEmailPublisher;
    private final GoogleAuthService googleAuthService;
    private final DeleteAccountEmailPublisher deleteAccountEmailPublisher;

    // ─────────────────────────────────────────────
    // Registro
    // ─────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>
     * Flujo:
     * <ol>
     * <li>Verifica que no exista un usuario ACTIVO con el mismo email.</li>
     * <li>Si existe un usuario INACTIVO con ese email, lo sobreescribe (actualiza
     * datos + OTP).</li>
     * <li>Si no existe ninguno, crea el usuario con status INACTIVO y
     * {@code isVerified=false}.</li>
     * <li>Genera un OTP de 4 dígitos y lo almacena en Redis con TTL.</li>
     * <li>Publica el evento de email de forma asíncrona (tras el commit).</li>
     * <li>Retorna solo el {@code userId}.</li>
     * </ol>
     */
    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = request.email();

        // 0. Rate-limit global de OTP por email
        otpComponent.checkAndIncrementOtpRate(email);

        // 1. Verificar que no exista un ACTIVO con ese email
        if (userRepository.existsByEmailAndStatus(email, UserStatusEnum.ACTIVO.getValue())) {
            throw new BusinessException("El email '" + email + "' ya está registrado y activo.");
        }

        UserEntity user;

        // 2. Si existe INACTIVO con ese email y activo (no eliminado), sobreescribir (permite re-registro
        // ilimitado hasta verificar)
        var existing = userRepository.findByEmailAndDeletedAtIsNull(email);
        if (existing.isPresent()) {
            user = existing.get();
            user.setName(toTitleCase(request.name()));
            user.setPhone(request.phone());
            user.setUbigeo(request.ubigeo());
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            user.setVerified(false);
            user.setDeletedAt(null);
            user = userRepository.save(user);
            log.info("Usuario INACTIVO sobreescrito para re-registro: {} (id={})", email, user.getId());
        } else {
            // 3. Crear nuevo usuario con status INACTIVO
            user = UserEntity.builder()
                    .name(toTitleCase(request.name()))
                    .email(email)
                    .phone(request.phone())
                    .ubigeo(request.ubigeo())
                    .passwordHash(passwordEncoder.encode(request.password()))
                    .roles(RoleEnum.CLIENT.name())
                    .build(); // status=INACTIVO, isVerified=false por @Builder.Default
            user = userRepository.save(user);
            log.info("Usuario INACTIVO registrado: {} (id={})", email, user.getId());
        }

        // 4. Generar OTP y almacenar en Redis
        String otpCode = otpComponent.generateAndStoreOtp(user.getId());

        // 5. Publicar evento para envío de email (asíncrono, tras commit)
        registerEmailPublisher.publishRegisterEmail(user.getName(), user.getEmail(), otpCode);

        return new RegisterResponse(user.getId());
    }

    // ─────────────────────────────────────────────
    // Verificación OTP
    // ─────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>
     * Flujo:
     * <ol>
     * <li>Busca el usuario INACTIVO no verificado por ID.</li>
     * <li>Valida el código OTP contra Redis.</li>
     * <li>Actualiza: {@code status=ACTIVO}, {@code isVerified=true},
     * {@code lastLoginAt=now()}.</li>
     * <li>Genera y retorna el par de tokens (auto-login).</li>
     * </ol>
     */
    @Override
    @Transactional
    public TokenResponse verifyOtp(VerifyOtpRequest request) {
        // 1. Buscar usuario INACTIVO no verificado
        UserEntity user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", request.userId()));

        if (user.getStatusEnum() != UserStatusEnum.INACTIVO || user.isVerified()) {
            throw new BusinessException("El usuario no está pendiente de verificación.");
        }

        // 2. Validar OTP en Redis
        if (!otpComponent.validateOtp(request.userId(), request.otpCode())) {
            throw new BusinessException("El código OTP es incorrecto o ha expirado.");
        }

        // 3. Activar usuario
        user.setStatusEnum(UserStatusEnum.ACTIVO);
        user.setVerified(true);
        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);
        log.info("Usuario verificado y activado: {} (id={})", user.getEmail(), user.getId());

        // 4. Generar y retornar tokens (auto-login post-verificación)
        return generateAndSaveTokenPair(user);
    }

    // ─────────────────────────────────────────────
    // Reenvío OTP
    // ─────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>
     * Flujo:
     * <ol>
     * <li>Busca el usuario INACTIVO no verificado por ID.</li>
     * <li>Verifica que el cooldown haya expirado.</li>
     * <li>Genera un nuevo OTP y lo almacena en Redis.</li>
     * <li>Publica el evento de email.</li>
     * </ol>
     */
    @Override
    @Transactional
    public MessageResponse resendOtp(ResendOtpRequest request) {
        // 1. Buscar usuario INACTIVO no verificado
        UserEntity user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", request.userId()));

        if (user.getStatusEnum() != UserStatusEnum.INACTIVO || user.isVerified()) {
            throw new BusinessException("El usuario no está pendiente de verificación.");
        }

        // 2. Rate-limit global de OTP por email
        otpComponent.checkAndIncrementOtpRate(user.getEmail());

        // 3. Verificar cooldown
        if (otpComponent.isCooldownActive(request.userId())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Debes esperar antes de volver a solicitar el código OTP.");
        }

        // 3. Generar nuevo OTP
        String otpCode = otpComponent.generateAndStoreOtp(request.userId());

        // 4. Publicar evento de email
        registerEmailPublisher.publishRegisterEmail(user.getName(), user.getEmail(), otpCode);
        log.info("OTP reenviado para usuario: {} (id={})", user.getEmail(), user.getId());

        return new MessageResponse("Código OTP reenviado exitosamente.");
    }

    // ─────────────────────────────────────────────
    // Forgot Password (recovery)
    // ─────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>
     * Flujo (seguro: no revela si el email existe):
     * <ol>
     * <li>Busca un usuario ACTIVO con el email proporcionado.</li>
     * <li>Si existe: genera OTP y publica evento de email de recovery.</li>
     * <li>Si no existe: no hace nada (no genera OTP ni envía email).</li>
     * <li>Siempre retorna el mismo mensaje genérico.</li>
     * </ol>
     */
    @Override
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        var userOpt = userRepository.findByEmailAndStatus(
                request.email(), UserStatusEnum.ACTIVO.getValue());

        // Rate-limit global de OTP por email (aplica aunque el email no exista)
        otpComponent.checkAndIncrementOtpRate(request.email());

        userOpt.ifPresent(user -> {
            // Respetar cooldown para evitar abuso
            if (!otpComponent.isCooldownActive(user.getId())) {
                String otpCode = otpComponent.generateAndStoreOtp(user.getId());
                recoveryEmailPublisher.publishRecoveryEmail(
                        user.getName(), user.getEmail(), otpCode);
                log.info("OTP de recovery generado para: {}", user.getEmail());
            } else {
                log.info("Cooldown activo para recovery de: {}", user.getEmail());
            }
        });

        // Respuesta genérica sin revelar existencia del email
        return new MessageResponse(
                "Si el correo está registrado, recibirás un código de verificación.");
    }

    // ─────────────────────────────────────────────
    // Verify Recovery OTP
    // ─────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>
     * Flujo:
     * <ol>
     * <li>Busca el usuario ACTIVO por ID.</li>
     * <li>Valida el OTP en Redis.</li>
     * <li>Si válido: genera un resetToken (UUID) con TTL 10 min en Redis.</li>
     * </ol>
     */
    @Override
    @Transactional(readOnly = true)
    public ResetTokenResponse verifyRecoveryOtp(VerifyRecoveryOtpRequest request) {
        UserEntity user = userRepository.findByEmailAndStatus(
                request.email(), UserStatusEnum.ACTIVO.getValue())
                .orElseThrow(() -> new BusinessException(
                        "El código OTP es incorrecto o ha expirado."));

        if (!otpComponent.validateOtp(user.getId(), request.otpCode())) {
            throw new BusinessException("El código OTP es incorrecto o ha expirado.");
        }

        String resetToken = otpComponent.generateResetToken(user.getId());
        log.info("Reset token generado para recovery de: {} (id={})",
                user.getEmail(), user.getId());

        return new ResetTokenResponse(resetToken);
    }

    // ─────────────────────────────────────────────
    // Reset Password
    // ─────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>
     * Flujo:
     * <ol>
     * <li>Valida y consume el resetToken en Redis (GET + DELETE).</li>
     * <li>Busca el usuario por ID.</li>
     * <li>Actualiza el hash de la contraseña con BCrypt.</li>
     * <li>Revoca todos los refresh tokens activos (fuerza re-login).</li>
     * </ol>
     */
    @Override
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        UUID userId = otpComponent.validateAndConsumeResetToken(request.resetToken());
        if (userId == null) {
            throw new BusinessException(
                    "Tu solicitud de recuperación ha expirado. Intenta nuevamente.");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Revocar todos los refresh tokens activos para forzar re-login
        refreshTokenRepository.revokeAllActiveByUserId(userId);

        log.info("Contraseña actualizada por recovery para: {} (id={})",
                user.getEmail(), userId);
        return new MessageResponse("Tu contraseña ha sido actualizada exitosamente.");
    }

    // ─────────────────────────────────────────────
    // Login
    // ─────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>
     * Flujo:
     * <ol>
     * <li>Busca el usuario ACTIVO por email.</li>
     * <li>Valida la contraseña con BCrypt.</li>
     * <li>Genera y retorna el par de tokens.</li>
     * </ol>
     */
    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        UserEntity user = findActiveUserByEmailOrThrow(request.email());
        validatePassword(request.password(), user.getPasswordHash());
        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);
        log.info("Login exitoso para: {}", user.getEmail());
        return generateAndSaveTokenPair(user);
    }

    // ─────────────────────────────────────────────
    // Admin Login
    // ─────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>
     * Mismo flujo que login, pero verifica que el usuario tenga rol ADMIN.
     * Falla con 403 si no tiene el rol requerido.
     */
    @Override
    @Transactional
    public TokenResponse adminLogin(LoginRequest request) {
        UserEntity user = findActiveUserByEmailOrThrow(request.email());
        validatePassword(request.password(), user.getPasswordHash());

        if (!user.hasRole(RoleEnum.ADMIN)) {
            throw new ForbiddenException("El usuario no tiene permisos de administrador.");
        }

        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);
        log.info("Admin login exitoso para: {}", user.getEmail());
        return generateAndSaveTokenPair(user);
    }

    // ─────────────────────────────────────────────
    // Refresh Token (rotación)
    // ─────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>
     * Flujo de rotación de tokens:
     * <ol>
     * <li>Calcula el hash SHA-256 del refresh token recibido.</li>
     * <li>Busca el token en BD (no revocado).</li>
     * <li>Valida que no esté expirado.</li>
     * <li>Valida la firma JWT del token.</li>
     * <li>Revoca el token anterior en BD ({@code revoked_at = now()}).</li>
     * <li>Genera un nuevo par de tokens.</li>
     * </ol>
     */
    @Override
    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.refreshToken());

        // 1. Buscar token activo en BD
        RefreshTokenEntity storedToken = refreshTokenRepository
                .findByTokenHashAndRevokedAtIsNull(tokenHash)
                .orElseThrow(() -> new BusinessException("Refresh token inválido o ya revocado."));

        // 2. Validar que no esté expirado
        if (storedToken.isExpired()) {
            throw new BusinessException("Refresh token expirado. Inicia sesión nuevamente.");
        }

        // 3. Validar la firma JWT (protección contra tokens falsificados)
        if (!jwtProvider.validateToken(request.refreshToken())) {
            throw new BusinessException("Refresh token con firma inválida.");
        }

        // 4. Revocar el token actual (rotación: un refresh token solo se usa una vez)
        storedToken.setRevokedAt(OffsetDateTime.now());
        refreshTokenRepository.save(storedToken);

        // 5. Cargar el usuario y generar el nuevo par de tokens
        UserEntity user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", storedToken.getUserId()));

        log.info("Refresh token rotado para usuario: {}", user.getEmail());
        return generateAndSaveTokenPair(user);
    }

    // ─────────────────────────────────────────────
    // Logout
    // ─────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>
     * Flujo de logout:
     * <ol>
     * <li>Extrae el token del request.</li>
     * <li>Extrae el userId del access token.</li>
     * <li>Calcula el TTL restante del access token y lo banea en Redis.</li>
     * <li>Revoca todos los refresh tokens activos del usuario en BD.</li>
     * </ol>
     */
    @Override
    @Transactional
    public MessageResponse logout(HttpServletRequest request) {
        String accessToken = extractTokenFromRequest(request);

        UUID userId = jwtProvider.getUserIdFromToken(accessToken);

        Date expiration = jwtProvider.getExpirationFromToken(accessToken);
        long nowMillis = DateUtilities.currentLocalDateTime()
                .atZone(DateUtilities.LIMA_ZONE)
                .toInstant()
                .toEpochMilli();
        long ttlMs = expiration.getTime() - nowMillis;

        tokenBanListComponent.banToken(accessToken, ttlMs);
        refreshTokenRepository.revokeAllActiveByUserId(userId);

        // Limpiar fb_token para no enviar notificaciones push a un dispositivo
        // deslogueado.
        // Al re-autenticarse, NotificationService en Flutter re-enviará el token
        // automáticamente.
        userRepository.findById(userId).ifPresent(user -> {
            user.setFbToken(null);
            userRepository.save(user);
        });

        log.info("Logout exitoso para usuario ID: {}", userId);
        return new MessageResponse("Session closed successfully");
    }

    /**
     * Extrae el token JWT del header Authorization del request.
     *
     * @param request request HTTP.
     * @return token JWT sin el prefijo "Bearer ".
     * @throws BadCredentialsException si el header no está presente o tiene formato
     *                                 inválido.
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        throw new BadCredentialsException("Token JWT no proporcionado en el header Authorization.");
    }

    // ─────────────────────────────────────────────
    // Update FB Token
    // ─────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>
     * Solo actualiza el campo {@code fb_token} del usuario autenticado.
     * No modifica ningún otro campo.
     *
     * @param userId  ID del usuario autenticado (del JWT).
     * @param request DTO con el nuevo token de Firebase.
     */
    @Override
    @Transactional
    public MessageResponse updateFbToken(UUID userId, UpdateFbTokenRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId));

        user.setFbToken(request.fbToken());
        userRepository.save(user);

        log.info("FB token actualizado para usuario ID: {}", userId);
        return new MessageResponse("FB token actualizado exitosamente.");
    }

    // ─────────────────────────────────────────────
    // GetMe
    // ─────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>
     * El userId ya fue validado por el {@code JwtAuthFilter} al procesar el token.
     */
    @Override
    @Transactional(readOnly = true)
    public MeResponse getMe(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId));

        List<String> roles = user.getRoleList().stream()
                .map(RoleEnum::name)
                .collect(Collectors.toList());

        return new MeResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getUbigeo(), roles);
    }

    // ─────────────────────────────────────────────
    // Métodos privados auxiliares
    // ─────────────────────────────────────────────

    /**
     * Genera un access token y un refresh token para el usuario indicado,
     * guarda el hash del refresh token en BD y retorna el par.
     *
     * @param user usuario autenticado.
     * @return par de tokens {@link TokenResponse}.
     */
    private TokenResponse generateAndSaveTokenPair(UserEntity user) {
        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        Date expiresAt = jwtProvider.getExpirationFromToken(refreshToken);

        RefreshTokenEntity refreshTokenEntity = RefreshTokenEntity.builder()
                .userId(user.getId())
                .tokenHash(hashToken(refreshToken))
                .expiresAt(expiresAt.toInstant().atOffset(java.time.ZoneOffset.UTC))
                .build();

        refreshTokenRepository.save(refreshTokenEntity);
        return new TokenResponse(accessToken, refreshToken);
    }

    /**
     * Busca un usuario ACTIVO por email.
     *
     * <p>
     * Lanza {@link BadCredentialsException} para no revelar si el email existe o
     * no.
     * Si el usuario existe pero está INACTIVO, también falla con credenciales
     * incorrectas.
     *
     * @param email email del usuario.
     * @return entidad del usuario ACTIVO encontrado.
     */
    private UserEntity findActiveUserByEmailOrThrow(String email) {
        return userRepository.findByEmailAndStatus(email, UserStatusEnum.ACTIVO.getValue())
                .orElseThrow(() -> new BadCredentialsException("Email o contraseña incorrectos."));
    }

    /**
     * Valida que la contraseña en texto plano coincida con el hash almacenado.
     *
     * @param rawPassword     contraseña enviada en el request.
     * @param encodedPassword hash BCrypt almacenado en la BD.
     * @throws BadCredentialsException si las contraseñas no coinciden.
     */
    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new BadCredentialsException("Email o contraseña incorrectos.");
        }
    }

    /**
     * Calcula el hash SHA-256 de un token JWT.
     *
     * <p>
     * Se usa para almacenar el refresh token en BD de forma segura.
     *
     * @param token token JWT en texto plano.
     * @return representación hexadecimal del hash SHA-256.
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al calcular el hash del token", e);
        }
    }

    /**
     * Convierte un texto a Title Case (primera letra de cada palabra en mayúscula).
     *
     * <p>
     * Ejemplo: {@code "aaron cisneros"} → {@code "Aaron Cisneros"}.
     *
     * @param text texto a transformar.
     * @return texto en Title Case, o el mismo valor si es null/vacío.
     */
    private String toTitleCase(String text) {
        if (text == null || text.isBlank())
            return text;
        String[] words = text.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0)
                sb.append(' ');
            sb.append(Character.toUpperCase(words[i].charAt(0)));
            if (words[i].length() > 1) {
                sb.append(words[i].substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    @Override
    @Transactional
    public TokenResponse loginWithGoogle(GoogleLoginRequest request) {
        log.info("Iniciando sesión con Google...");
        // 1. Validar Google Token
        GoogleIdToken.Payload payload = googleAuthService.verifyToken(request.idToken());
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        // 2. Buscar si ya existe el usuario activo (no eliminado)
        UserEntity user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseGet(() -> {
                    log.info("Usuario de Google no registrado. Registrando automáticamente: {}", email);
                    // Registrar nuevo usuario auto-verificado y activo
                    UserEntity newUser = UserEntity.builder()
                            .name(toTitleCase(name))
                            .email(email)
                            // Generamos una contraseña aleatoria de un solo uso para cumplir con NOT NULL
                            .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                            .status(UserStatusEnum.ACTIVO.getValue())
                            .isVerified(true)
                            .roles(RoleEnum.CLIENT.name())
                            .build();
                    return userRepository.save(newUser);
                });

        // 3. Si estaba registrado pero INACTIVO, lo activamos
        if (user.getStatusEnum() == UserStatusEnum.INACTIVO || !user.isVerified()) {
            log.info("Activando cuenta del usuario {} autenticado con Google", email);
            user.setStatusEnum(UserStatusEnum.ACTIVO);
            user.setVerified(true);
            user.setDeletedAt(null);
        }

        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        log.info("Login con Google exitoso para usuario: {} (id={})", user.getEmail(), user.getId());
        return generateAndSaveTokenPair(user);
    }

    @Override
    @Transactional
    public MessageResponse initiateDeleteAccount(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId));

        // 1. Rate-limit global de OTP por email
        otpComponent.checkAndIncrementOtpRate(user.getEmail());

        // 2. Verificar cooldown
        if (otpComponent.isCooldownActive(userId)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Debes esperar antes de volver a solicitar el código de verificación.");
        }

        // 3. Generar OTP y almacenar en Redis
        String otpCode = otpComponent.generateAndStoreOtp(userId);

        // 4. Publicar evento para envío de email
        deleteAccountEmailPublisher.publishDeleteAccountEmail(user.getName(), user.getEmail(), otpCode);

        log.info("Proceso de eliminación de cuenta iniciado para: {} (id={})", user.getEmail(), userId);
        return new MessageResponse("Se ha enviado un código de verificación a tu correo electrónico.");
    }

    @Override
    @Transactional
    public MessageResponse verifyDeleteAccount(UUID userId, VerifyDeleteRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId));

        // 1. Validar OTP en Redis
        if (!otpComponent.validateOtp(userId, request.otpCode())) {
            throw new BusinessException("El código OTP es incorrecto o ha expirado.");
        }

        // 2. Eliminar tokens de refresco del usuario
        refreshTokenRepository.revokeAllActiveByUserId(userId);

        // 3. Borrado lógico: desactivar usuario y registrar fecha de baja
        user.setStatusEnum(UserStatusEnum.INACTIVO);
        user.setVerified(false);
        user.setDeletedAt(OffsetDateTime.now());
        userRepository.save(user);

        log.info("Cuenta desactivada (soft delete) para el usuario: {} (id={})", user.getEmail(), userId);
        return new MessageResponse("Tu cuenta ha sido eliminada permanentemente.");
    }
}
