package com.sportsbooking.mainapi.config;

import com.sportsbooking.mainapi.clients.EmailApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Configuración de clientes HTTP declarativos (HTTP Interfaces de Spring Framework 7).
 *
 * <p>Crea los beans de tipo {@link EmailApiClient} usando {@code RestClient}
 * + {@code HttpServiceProxyFactory}. No requiere Feign ni Spring Cloud.
 *
 * <p>La URL base se inyecta desde el YAML: {@code feign-clients.email-api.base-url}.
 */
@Slf4j
@Configuration
public class HttpClientConfig {

    /**
     * Bean del cliente HTTP para la EMAIL-API.
     *
     * @param baseUrl URL base de EMAIL-API, configurada en YAML.
     * @return proxy de {@link EmailApiClient} listo para inyectar.
     */
    @Bean
    public EmailApiClient emailApiClient(
            @Value("${feign-clients.email-api.base-url}") String baseUrl) {
        log.info("Inicializando EmailApiClient con base-url: {}", baseUrl);

        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();

        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(EmailApiClient.class);
    }
}
