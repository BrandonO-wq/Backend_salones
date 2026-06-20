package com.sportsbooking.mainapi.components.listener;

import com.sportsbooking.mainapi.components.email.RecoveryEmail;
import com.sportsbooking.mainapi.components.event.RecoveryEmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener asíncrono del evento {@link RecoveryEmailEvent}.
 *
 * <p>Se ejecuta de forma asíncrona ({@code @Async}) DESPUÉS del commit de la transacción
 * ({@code AFTER_COMMIT}).
 *
 * <p>Si el envío de email falla, el error se captura y se loguea sin afectar
 * el flujo de recovery.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecoveryEmailListener {

    private final RecoveryEmail recoveryEmail;

    /**
     * Recibe el evento y delega el envío del email OTP de recovery.
     *
     * @param event evento con datos del usuario y el código OTP.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRecoveryEmail(RecoveryEmailEvent event) {
        log.info("Procesando envío de OTP de recovery por email para: {}", event.userEmail());
        try {
            recoveryEmail.send(event);
        } catch (Exception e) {
            log.error("Error al enviar email OTP de recovery a {} (ignorado): {}",
                    event.userEmail(), e.getMessage(), e);
        }
    }
}
