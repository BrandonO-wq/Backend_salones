package com.sportsbooking.mainapi.dto.client.emailapi;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * DTO para enviar solicitudes de email a la EMAIL-API.
 *
 * <p>Contrato del endpoint {@code POST /email} de la EMAIL-API.
 * El campo {@code templateName} indica la plantilla Thymeleaf a usar,
 * y {@code templateVariables} son las variables inyectadas en dicha plantilla.
 */
@Data
@Builder
public class EmailRequestDto {

    /** Destinatarios principales. */
    private List<String> to;

    /** Destinatarios en copia. Opcional. */
    private List<String> cc;

    /** Destinatarios en copia oculta. Opcional. */
    private List<String> bcc;

    /** Asunto del correo. */
    private String subject;

    /** Nombre del template Thymeleaf (ej: {@code "OTP-REGISTER.html"}). */
    private String templateName;

    /** Variables inyectadas en el template Thymeleaf. */
    private Map<String, Object> templateVariables;
}
