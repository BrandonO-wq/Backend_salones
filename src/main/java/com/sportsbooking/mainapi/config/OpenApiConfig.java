package com.sportsbooking.mainapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ForwardedHeaderFilter;

import java.util.List;

/**
 * Configuración de Swagger/OpenAPI para la documentación de la API.
 *
 * <p>Define:
 * <ul>
 *   <li>Información general de la API (título, versión, descripción).</li>
 *   <li>Esquema de seguridad {@code Bearer JWT} para autenticar endpoints desde Swagger UI.</li>
 *   <li>Soporte para proxy inverso (Nginx) con {@link ForwardedHeaderFilter}.</li>
 * </ul>
 *
 * <p>Para usar Swagger UI, acceder a: {@code /swagger-ui/index.html}
 */
@Configuration
public class OpenApiConfig {

    /** URL pública del servidor. Si está vacía, Swagger usa la URL relativa (local). */
    @Value("${app.public-url:}")
    private String publicUrl;

    /**
     * Construye y configura la instancia de OpenAPI con información de la API
     * y el esquema de autenticación JWT.
     *
     * <p>Si {@code app.public-url} está configurada, agrega el server URL
     * para que Swagger UI haga requests al dominio correcto detrás del proxy.
     *
     * @return configuración de OpenAPI.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("Sports Booking - Main API")
                        .version("1.0.0")
                        .description("API principal - Sports Booking. " +
                                "Usar el botón 'Authorize' e ingresar el token JWT para acceder a los endpoints protegidos.")
                )
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Ingresar el access token JWT obtenido en login/register.")
                        )
                );

        // Si hay URL pública configurada (QA/Prod), fijarla como server URL
        if (publicUrl != null && !publicUrl.isBlank()) {
            openAPI.servers(List.of(
                    new Server().url(publicUrl).description("Servidor principal")
            ));
        }

        return openAPI;
    }

    /**
     * Filtro que procesa los headers {@code X-Forwarded-*} enviados por Nginx.
     *
     * <p>Necesario para que Spring Boot genere URLs correctas (HTTPS en lugar de HTTP)
     * cuando está detrás de un proxy inverso.
     *
     * @return filtro de headers forwarded.
     */
    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }
}

