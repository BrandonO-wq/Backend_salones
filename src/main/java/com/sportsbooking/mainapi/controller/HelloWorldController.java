package com.sportsbooking.mainapi.controller;

import com.sportsbooking.mainapi.dto.common.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador de prueba para verificar que la API está corriendo.
 *
 * <p><strong>Solo para uso en perfil {@code local}.</strong>
 * En producción las rutas de este controlador estarán bajo autenticación,
 * por lo que solo son útiles para comprobar el funcionamiento básico del servidor.
 *
 * <p>En perfil local ({@code SPRING_PROFILES_ACTIVE=local}), todas las rutas
 * son accesibles sin JWT gracias al {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/hello")
@Tag(name = "Hello World", description = "Endpoint de prueba - solo útil en perfil local")
public class HelloWorldController {

    /**
     * Endpoint de prueba que confirma que la API está activa.
     *
     * <p>En perfil local: accesible sin JWT.
     * En otros perfiles: requiere JWT (útil para verificar que el token funciona).
     *
     * @return mensaje de bienvenida.
     */
    @GetMapping
    @Operation(
        summary = "Prueba de conexión",
        description = "Verifica que la API está corriendo. En perfil local no requiere JWT."
    )
    public ResponseEntity<MessageResponse> helloWorld() {
        return ResponseEntity.ok(new MessageResponse("Hola Mundo - CanchaGo API"));
    }
}
