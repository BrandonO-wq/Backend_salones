package com.sportsbooking.mainapi.components.email;

import com.sportsbooking.mainapi.clients.EmailApiClient;
import com.sportsbooking.mainapi.config.OtpProperties;
import com.sportsbooking.mainapi.dto.client.emailapi.EmailRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Componente responsable de construir y enviar el email de OTP para
 * solicitar el cambio de configuración del ciclo de comisiones.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CycleEmail {

    private static final String TEMPLATE_NAME = "OTP-CYCLE.html";
    private static final String APP_NAME = "EventPlace";

    private final EmailApiClient emailApiClient;
    private final OtpProperties otpProperties;

    /**
     * Construye y envía el email OTP de cambio de ciclo.
     */
    public void send(String userName, String userEmail, String otpCode) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", userName);
        variables.put("otpCode", otpCode);
        variables.put("otpDuration", String.valueOf(otpProperties.getExpirationMinutes()));
        variables.put("appName", APP_NAME);

        EmailRequestDto request = EmailRequestDto.builder()
                .to(List.of(userEmail))
                .subject("Confirmación de cambio de ciclo de comisiones - " + APP_NAME)
                .templateName(TEMPLATE_NAME)
                .templateVariables(variables)
                .build();

        log.info("Enviando email OTP de cambio de ciclo de comisiones a: {}", userEmail);
        emailApiClient.sendEmail(request);
    }
}
