package com.sportsbooking.mainapi.components.email;

import com.sportsbooking.mainapi.clients.EmailApiClient;
import com.sportsbooking.mainapi.components.event.DeleteAccountEmailEvent;
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
 * confirmación de eliminación de cuenta.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteAccountEmail {

    private static final String TEMPLATE_NAME = "OTP-DELETE.html";
    private static final String APP_NAME = "EventPlace";

    private final EmailApiClient emailApiClient;
    private final OtpProperties otpProperties;

    /**
     * Construye y envía el email de confirmación de eliminación de cuenta.
     *
     * @param event evento con nombre, email y código OTP del usuario.
     */
    public void send(DeleteAccountEmailEvent event) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", event.userName());
        variables.put("otpCode", event.otpCode());
        variables.put("otpDuration", String.valueOf(otpProperties.getExpirationMinutes()));
        variables.put("appName", APP_NAME);

        EmailRequestDto request = EmailRequestDto.builder()
                .to(List.of(event.userEmail()))
                .subject("Confirmación de eliminación de cuenta - " + APP_NAME)
                .templateName(TEMPLATE_NAME)
                .templateVariables(variables)
                .build();

        log.info("Enviando email OTP de confirmación de eliminación a: {}", event.userEmail());
        emailApiClient.sendEmail(request);
    }
}
