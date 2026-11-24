package com.sportsbooking.mainapi.components.publisher;

import com.sportsbooking.mainapi.components.event.DeleteAccountEmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Componente encargado de publicar el evento {@link DeleteAccountEmailEvent}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteAccountEmailPublisher {

    private final ApplicationEventPublisher publisher;

    /**
     * Publica el evento de email de confirmación de eliminación de cuenta.
     *
     * @param userName  nombre completo del usuario.
     * @param userEmail email del usuario.
     * @param otpCode   código OTP generado.
     */
    public void publishDeleteAccountEmail(String userName, String userEmail, String otpCode) {
        log.debug("Publicando DeleteAccountEmailEvent para: {}", userEmail);
        publisher.publishEvent(new DeleteAccountEmailEvent(userName, userEmail, otpCode));
    }
}
