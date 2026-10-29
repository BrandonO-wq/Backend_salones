package com.sportsbooking.mainapi.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

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

        @Value("${app.cors.allowed-origins:http://localhost:4200,http://localhost:8080,https://oryzentprueba.com,https://www.oryzentprueba.com,https://intranet.oryzentprueba.com,https://www.intranet.oryzentprueba.com}")
        private String[] allowedOrigins;

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
                        "/auth/google",
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
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                                                // Rutas públicas (GET/SEARCH/IN-BOUNDS) ya declaradas abajo.
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/venues/{id}")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/venues/report/bookings")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/venues/report/stats")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/venues/report/detailed")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/bookings/stats/by-event-type")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/bookings/report/event-types")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/bookings/admin/event-types-count")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/bookings/admin/stats/event-types")
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

        /**
         * Bean para la configuración global de CORS.
         * <p>
         * Lee los orígenes permitidos desde {@code application.yml}
         * ({@code app.cors.allowed-origins}).
         *
         * @return la fuente de configuración de CORS.
         */
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                // Combinar los orígenes inyectados con patrones de ngrok para desarrollo
                List<String> origins = new java.util.ArrayList<>(List.of(allowedOrigins));
                origins.add("https://*.ngrok-free.app");
                origins.add("https://*.ngrok.io");

                configuration.setAllowedOriginPatterns(origins);
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(
                                Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept",
                                                "ngrok-skip-browser-warning"));
                configuration.setExposedHeaders(List.of("Authorization"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}
