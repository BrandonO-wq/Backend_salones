package com.sportsbooking.mainapi.components.publisher;

import com.sportsbooking.mainapi.components.event.RecoveryEmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Componente encargado de publicar el evento {@link RecoveryEmailEvent}.
 *
 * <p>Utiliza {@link ApplicationEventPublisher} de Spring para desacoplar
 * el trigger del envío de email de la lógica de recovery.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecoveryEmailPublisher {

    private final ApplicationEventPublisher publisher;

    /**
     * Publica el evento de email de recuperación de contraseña.
     *
     * @param userName  nombre completo del usuario.
     * @param userEmail email del usuario.
     * @param otpCode   código OTP generado.
     */
    public void publishRecoveryEmail(String userName, String userEmail, String otpCode) {
        log.debug("Publicando RecoveryEmailEvent para: {}", userEmail);
        publisher.publishEvent(new RecoveryEmailEvent(userName, userEmail, otpCode));
    }
}
