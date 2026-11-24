package com.sportsbooking.mainapi.common;

import com.sportsbooking.mainapi.common.exception.BadCredentialsException;
import com.sportsbooking.mainapi.common.exception.BusinessException;
import com.sportsbooking.mainapi.common.exception.ForbiddenException;
import com.sportsbooking.mainapi.common.exception.ResourceNotFoundException;
import com.sportsbooking.mainapi.common.exception.UnauthorizedException;
import com.sportsbooking.mainapi.dto.common.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para toda la API.
 *
 * <p>
 * Centraliza el manejo de errores en un solo lugar, garantizando que todos los
 * errores devuelvan el formato estándar {@link ErrorResponse}.
 *
 * <p>
 * Niveles de log:
 * <ul>
 * <li>WARN – errores de negocio esperados (credenciales, validación,
 * permisos).</li>
 * <li>ERROR – errores del sistema no esperados (excepciones genéricas).</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

        /**
         * Maneja errores de validación de los campos del request body.
         * HTTP 400 - Bad Request.
         *
         * <p>
         * Se dispara cuando un DTO con {@code @Valid} falla la validación.
         *
         * @param ex excepción con los detalles de los campos inválidos.
         * @return lista de mensajes de error por campo.
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationError(MethodArgumentNotValidException ex) {
                List<String> details = ex.getBindingResult().getFieldErrors().stream()
                                .map(FieldError::getDefaultMessage)
                                .collect(Collectors.toList());

                log.warn("Error de validación: {}", details);

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(new ErrorResponse("VALIDATION_ERROR", "Los datos enviados no son válidos",
                                                details));
        }

        /**
         * Maneja intentos de login con credenciales incorrectas.
         * HTTP 401 - Unauthorized.
         *
         * @param ex excepción con el mensaje de error.
         * @return mensaje de error de autenticación.
         */
        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
                log.warn("Credenciales inválidas: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(new ErrorResponse("BAD_CREDENTIALS", ex.getMessage(), null));
        }

        /**
         * Maneja requests a endpoints protegidos con token expirado o ausente.
         * HTTP 401 - Unauthorized.
         *
         * @param ex excepción lanzada desde el controller al detectar authentication
         *           null.
         * @return mensaje de error de autenticación.
         */
        @ExceptionHandler(UnauthorizedException.class)
        public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
                log.warn("Acceso no autorizado: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(new ErrorResponse("UNAUTHORIZED", ex.getMessage(), null));
        }

        /**
         * Maneja intentos de acceso a recursos sin permisos suficientes.
         * HTTP 403 - Forbidden.
         *
         * @param ex excepción con el mensaje de acceso denegado.
         * @return mensaje de error de acceso denegado.
         */
        @ExceptionHandler(ForbiddenException.class)
        public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
                log.warn("Acceso denegado: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.FORBIDDEN)
                                .body(new ErrorResponse("ACCESS_DENIED", ex.getMessage(), null));
        }

        /**
         * Maneja recursos no encontrados en la BD.
         * HTTP 404 - Not Found.
         *
         * @param ex excepción con el recurso e identificador buscado.
         * @return mensaje de recurso no encontrado.
         */
        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
                log.warn("Recurso no encontrado: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(new ErrorResponse("RESOURCE_NOT_FOUND", ex.getMessage(), null));
        }

        /**
         * Maneja rutas o URLs que no existen en el sistema (Spring Boot 3).
         * HTTP 404 - Not Found.
         *
         * @param ex excepción con el path inválido.
         * @return mensaje de recurso no encontrado genérico.
         */
        @ExceptionHandler(NoResourceFoundException.class)
        public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
                log.warn("Ruta no encontrada: {}", ex.getResourcePath());
                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(new ErrorResponse("URL_NOT_FOUND",
                                                "La ruta solicitada no existe o no está mapeada", null));
        }

        /**
         * Maneja violaciones de reglas de negocio.
         * HTTP 409 - Conflict.
         *
         * <p>
         * Ejemplo: email ya registrado, refresh token revocado.
         *
         * @param ex excepción con el detalle de la violación.
         * @return mensaje de conflicto de negocio.
         */
        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
                log.warn("Violación de regla de negocio: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(new ErrorResponse("BUSINESS_ERROR", ex.getMessage(), null));
        }

        /**
         * Maneja excepciones con código HTTP dinámico (ej: 429 Too Many Requests).
         *
         * <p>
         * Se usa en flujos como el rate-limit de OTP y cooldown de reenvío.
         *
         * @param ex excepción con status HTTP y reason.
         * @return mensaje de error con el reason de la excepción.
         */
        @ExceptionHandler(ResponseStatusException.class)
        public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
                HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
                if (status == null)
                        status = HttpStatus.INTERNAL_SERVER_ERROR;

                String code = status == HttpStatus.TOO_MANY_REQUESTS
                                ? "RATE_LIMIT_EXCEEDED"
                                : "HTTP_ERROR";

                log.warn("ResponseStatusException [{}]: {}", status.value(), ex.getReason());
                return ResponseEntity
                                .status(status)
                                .body(new ErrorResponse(code, ex.getReason(), null));
        }

        /**
         * Maneja errores de argumentos inválidos (validación manual).
         * HTTP 400 - Bad Request.
         *
         * @param ex excepción con el mensaje de error.
         * @return mensaje de error de validación.
         */
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
                log.warn("Error de argumento: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(new ErrorResponse("INVALID_ARGUMENT", ex.getMessage(), null));
        }

        /**
         * Maneja cualquier excepción no controlada explícitamente.
         * HTTP 500 - Internal Server Error.
         *
         * <p>
         * El mensaje de error retornado al cliente es genérico para no exponer
         * detalles técnicos internos.
         *
         * @param ex excepción inesperada.
         * @return mensaje de error interno genérico.
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
                log.error("Error interno inesperado: {}", ex.getMessage(), ex);
                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(new ErrorResponse("INTERNAL_ERROR",
                                                "Ocurrió un error interno. Contacte al administrador.", null));
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
                ErrorResponse err = new ErrorResponse("ACCESS_DENIED", ex.getMessage(), List.of());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err);
        }

}
