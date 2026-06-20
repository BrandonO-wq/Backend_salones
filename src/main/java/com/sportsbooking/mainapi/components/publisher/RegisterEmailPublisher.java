package com.sportsbooking.mainapi.components.publisher;

import com.sportsbooking.mainapi.components.event.RegisterEmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Componente encargado de publicar el evento {@link RegisterEmailEvent}.
 *
 * <p>Utiliza {@link ApplicationEventPublisher} de Spring para desacoplar
 * el trigger del envío de email de la lógica de registro.
 *
 * <p>El evento es procesado de forma asíncrona por {@link com.sportsbooking.mainapi.components.listener.RegisterEmailListener}
 * tras el commit de la transacción.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegisterEmailPublisher {

    private final ApplicationEventPublisher publisher;

    /**
     * Publica el evento de email de registro OTP.
     *
     * @param userName  nombre completo del usuario.
     * @param userEmail email del usuario.
     * @param otpCode   código OTP generado.
     */
    public void publishRegisterEmail(String userName, String userEmail, String otpCode) {
        log.debug("Publicando RegisterEmailEvent para: {}", userEmail);
        publisher.publishEvent(new RegisterEmailEvent(userName, userEmail, otpCode));
    }
}
