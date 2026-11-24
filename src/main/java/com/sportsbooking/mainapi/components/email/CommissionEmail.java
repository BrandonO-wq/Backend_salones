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
 * solicitar el cambio de comisión del sistema.
 *
 * <p>
 * Consume la EMAIL-API mediante {@link EmailApiClient} y usa la plantilla
 * Thymeleaf {@code OTP-COMMISSION.html}, la cual debe existir en la EMAIL-API.
 *
 * <p>
 * Variables inyectadas en el template:
 * <ul>
 * <li>{@code userName} – nombre del administrador.</li>
 * <li>{@code otpCode} – código OTP de 4 dígitos.</li>
 * <li>{@code otpDuration} – minutos de vigencia del OTP.</li>
 * <li>{@code appName} – nombre de la aplicación ("EventPlace").</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommissionEmail {

    private static final String TEMPLATE_NAME = "OTP-COMMISSION.html";
    private static final String APP_NAME = "EventPlace";

    private final EmailApiClient emailApiClient;
    private final OtpProperties otpProperties;

    /**
     * Construye y envía el email OTP de cambio de comisión.
     *
     * @param userName  nombre del administrador.
     * @param userEmail email del administrador.
     * @param otpCode   código OTP generado.
     */
    public void send(String userName, String userEmail, String otpCode) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", userName);
        variables.put("otpCode", otpCode);
        variables.put("otpDuration", String.valueOf(otpProperties.getExpirationMinutes()));
        variables.put("appName", APP_NAME);

        EmailRequestDto request = EmailRequestDto.builder()
                .to(List.of(userEmail))
                .subject("Confirmación de cambio de comisión - " + APP_NAME)
                .templateName(TEMPLATE_NAME)
                .templateVariables(variables)
                .build();

        log.info("Enviando email OTP de cambio de comisión a: {}", userEmail);
        emailApiClient.sendEmail(request);
    }
}
