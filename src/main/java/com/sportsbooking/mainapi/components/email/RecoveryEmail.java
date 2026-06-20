package com.sportsbooking.mainapi.components.email;

import com.sportsbooking.mainapi.clients.EmailApiClient;
import com.sportsbooking.mainapi.components.event.RecoveryEmailEvent;
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
 * recuperación de contraseña.
 *
 * <p>
 * Consume la EMAIL-API mediante {@link EmailApiClient} y usa la plantilla
 * Thymeleaf {@code OTP-RECOVERY.html}, la cual debe existir en la EMAIL-API.
 *
 * <p>
 * Variables inyectadas en el template:
 * <ul>
 * <li>{@code userName} – nombre completo del usuario.</li>
 * <li>{@code otpCode} – código OTP de 4 dígitos.</li>
 * <li>{@code otpDuration} – minutos de vigencia del OTP (configurable en
 * YAML).</li>
 * <li>{@code appName} – nombre de la aplicación ("EventPlace").</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecoveryEmail {

    private static final String TEMPLATE_NAME = "OTP-RECOVERY.html";
    private static final String APP_NAME = "EventPlace";

    private final EmailApiClient emailApiClient;
    private final OtpProperties otpProperties;

    /**
     * Construye y envía el email de recuperación de contraseña.
     *
     * @param event evento con nombre, email y código OTP del usuario.
     */
    public void send(RecoveryEmailEvent event) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", event.userName());
        variables.put("otpCode", event.otpCode());
        variables.put("otpDuration", String.valueOf(otpProperties.getExpirationMinutes()));
        variables.put("appName", APP_NAME);

        EmailRequestDto request = EmailRequestDto.builder()
                .to(List.of(event.userEmail()))
                .subject("Recuperación de contraseña - " + APP_NAME)
                .templateName(TEMPLATE_NAME)
                .templateVariables(variables)
                .build();

        log.info("Enviando email OTP de recovery a: {}", event.userEmail());
        emailApiClient.sendEmail(request);
    }
}
