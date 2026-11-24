package com.sportsbooking.mainapi.components.listener;

import com.sportsbooking.mainapi.components.email.RegisterEmail;
import com.sportsbooking.mainapi.components.event.RegisterEmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener asíncrono del evento {@link RegisterEmailEvent}.
 *
 * <p>Se ejecuta de forma asíncrona ({@code @Async}) DESPUÉS del commit de la transacción
 * ({@code AFTER_COMMIT}), garantizando que el usuario ya esté persistido en BD
 * antes de intentar enviar el email.
 *
 * <p>Si el envío de email falla, el error se captura y se loguea sin afectar
 * el flujo de registro del usuario.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegisterEmailListener {

    private final RegisterEmail registerEmail;

    /**
     * Recibe el evento y delega el envío del email OTP al componente {@link RegisterEmail}.
     *
     * @param event evento con datos del usuario y el código OTP.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRegisterEmail(RegisterEmailEvent event) {
        log.info("Procesando envío de OTP por email para: {}", event.userEmail());
        try {
            registerEmail.send(event);
        } catch (Exception e) {
            log.error("Error al enviar email OTP de registro a {} (ignorado): {}",
                    event.userEmail(), e.getMessage(), e);
        }
    }
}
