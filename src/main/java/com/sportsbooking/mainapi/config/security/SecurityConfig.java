package com.sportsbooking.mainapi.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración principal de Spring Security 7.
 *
 * <p>
 * Se requiere JWT para la mayoría de rutas, excepto las de autenticación
 * pública
 * ({@code /auth/register}, {@code /auth/login}, etc.) y Swagger.
 *
 * <p>
 * Características:
 * <ul>
 * <li>CSRF deshabilitado: la API es stateless y usa JWT.</li>
 * <li>Sesión stateless: no se usa la sesión HTTP de Spring.</li>
 * <li>Password encoder BCrypt con factor de coste 4.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthFilter jwtAuthFilter;
        private final CustomAuthEntryPoint customAuthEntryPoint;
        private final CustomAccessDeniedHandler customAccessDeniedHandler;

        /** Rutas públicas que no requieren autenticación. */
        private static final String[] PUBLIC_ENDPOINTS = {
                        "/auth/register",
                        "/auth/verify-account",
                        "/auth/resend-otp",
                        "/auth/forgot-password",
                        "/auth/verify-recovery-otp",
                        "/auth/reset-password",
                        "/auth/login",
                        "/auth/admin/login",
                        "/auth/refresh",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
        };

        /**
         * Define el {@link SecurityFilterChain} principal de la aplicación.
         *
         * <p>
         * Las rutas públicas no requieren JWT. Todas las demás requieren
         * un token válido. El filtro JWT se ejecuta antes de la autenticación
         * estándar de Spring para poblar el SecurityContext.
         *
         * @param http builder de configuración de seguridad HTTP.
         * @return cadena de filtros de seguridad configurada.
         */
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                                                // Rutas públicas (GET/SEARCH/IN-BOUNDS) ya declaradas abajo.
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/venues/{id}")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/venues/in-bounds")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/venues/search")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/venues/photos/{photoId}")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET, "/ubigeos")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/venues/{venueId}/reviews")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/venues/{venueId}/reviews/summary")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .exceptionHandling(ex -> ex.authenticationEntryPoint(customAuthEntryPoint)
                                                .accessDeniedHandler(customAccessDeniedHandler))
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        /**
         * Bean de codificador de contraseñas usando BCrypt con factor de coste 4.
         *
         * @return instancia de {@link BCryptPasswordEncoder}.
         */
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder(4);
        }
}
