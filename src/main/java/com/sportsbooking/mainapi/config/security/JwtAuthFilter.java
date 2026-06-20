package com.sportsbooking.mainapi.config.security;

import com.sportsbooking.mainapi.components.JwtProviderComponent;
import com.sportsbooking.mainapi.components.TokenBanListComponent;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Filtro de seguridad que valida el token JWT en cada request.
 *
 * <p>Se ejecuta una vez por request ({@link OncePerRequestFilter}) y realiza:
 * <ol>
 *   <li>Extracción del token del header {@code Authorization: Bearer <token>}.</li>
 *   <li>Verificación de que el token no esté en la ban list de Redis.</li>
 *   <li>Validación de la firma y expiración del token.</li>
 *   <li>Establecimiento del contexto de seguridad con el usuario autenticado.</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_HEADER   = "Authorization";

    private final JwtProviderComponent  jwtProvider;
    private final TokenBanListComponent tokenBanListService;

    /**
     * Ejecuta la validación del JWT para cada request entrante.
     *
     * <p>Si el token es válido y no está baneado, establece el {@link SecurityContextHolder}
     * con los datos del usuario, permitiendo que Spring Security autorice el acceso.
     *
     * @param request     request HTTP entrante.
     * @param response    response HTTP saliente.
     * @param filterChain cadena de filtros a continuar.
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null && processToken(token, request)) {
            log.debug("Token JWT válido para request: {}", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Procesa el token: verifica ban list, valida firma y establece el SecurityContext.
     *
     * @param token   token JWT extraído del header.
     * @param request request HTTP actual.
     * @return {@code true} si el token fue válido y el contexto fue establecido.
     */
    private boolean processToken(String token, HttpServletRequest request) {

        // 1. Verificar que el token no fue baneado por un logout previo
        if (tokenBanListService.isTokenBanned(token)) {
            log.warn("Intento de uso de token baneado: {}", request.getRequestURI());
            return false;
        }

        // 2. Validar firma y expiración del token
        if (!jwtProvider.validateToken(token)) {
            return false;
        }

        // 3. Extraer datos del token y establecer el contexto de seguridad
        UUID userId = jwtProvider.getUserIdFromToken(token);
        List<String> roles = jwtProvider.getRolesFromToken(token);

        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        // Se usa el userId como principal para poder recuperarlo fácilmente en los controllers
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        return true;
    }

    /**
     * Extrae el token JWT del header {@code Authorization}.
     *
     * @param request request HTTP.
     * @return el token JWT sin el prefijo "Bearer ", o {@code null} si no está presente.
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
