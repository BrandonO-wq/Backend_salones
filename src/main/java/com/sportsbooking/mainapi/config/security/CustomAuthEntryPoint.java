package com.sportsbooking.mainapi.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbooking.mainapi.dto.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Punto de entrada para errores de autenticación en Spring Security.
 *
 * <p>
 * Se invoca automáticamente cuando un request llega a un endpoint protegido
 * sin un token JWT válido o con un token expirado/baneado.
 *
 * <p>
 * Retorna una respuesta JSON con formato estándar {@link ErrorResponse} y HTTP
 * 401.
 */
@Component
public class CustomAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Genera una respuesta JSON de error 401 cuando la autenticación falla.
     *
     * @param request       request HTTP entrante.
     * @param response      response HTTP donde se escribe el error.
     * @param authException excepción de autenticación que disparó este punto de
     *                      entrada.
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        String authHeader = request.getHeader("Authorization");
        String message;

        if (authHeader == null || authHeader.isEmpty()) {
            message = "Falta header Authorization. Agrega 'Authorization: Bearer <token>'";
        } else if (!authHeader.startsWith("Bearer ")) {
            message = "Header Authorization malformado. Debe empezar con 'Bearer '";
        } else {
            // Mensaje genérico para no filtrar detalles sensibles
            message = "Token inválido o expirado. Vuelve a autenticarte.";
        }

        ErrorResponse error = new ErrorResponse(
                "UNAUTHORIZED",
                message,
                List.of(authException == null ? "" : authException.getMessage()));

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), error);
    }
}
