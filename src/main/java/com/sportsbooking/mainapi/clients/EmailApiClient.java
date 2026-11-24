package com.sportsbooking.mainapi.clients;

import com.sportsbooking.mainapi.dto.client.emailapi.EmailRequestDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * Cliente HTTP declarativo para la EMAIL-API, usando HTTP Interfaces de Spring Framework 7.
 *
 * <p>La URL base se configura en {@code feign-clients.email-api.base-url} (YAML)
 * y el bean se crea en {@link com.sportsbooking.mainapi.config.HttpClientConfig}.
 *
 * <p>No requiere dependencias externas (ni Feign ni Spring Cloud):
 * usa {@code RestClient} + {@code HttpServiceProxyFactory} nativos de Spring Framework 7.
 */
@HttpExchange
public interface EmailApiClient {

    /**
     * Envía un email a través de la EMAIL-API.
     *
     * <p>La EMAIL-API procesa la solicitud de forma asíncrona y retorna {@code HTTP 202 Accepted}.
     *
     * @param request datos del email (destinatarios, asunto, template, variables).
     * @return respuesta HTTP de la EMAIL-API.
     */
    @PostExchange("/email")
    ResponseEntity<Void> sendEmail(@RequestBody EmailRequestDto request);
}
