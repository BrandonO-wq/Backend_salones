package com.sportsbooking.mainapi.components.listener;

import com.sportsbooking.mainapi.components.email.DeleteAccountEmail;
import com.sportsbooking.mainapi.components.event.DeleteAccountEmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener asíncrono del evento {@link DeleteAccountEmailEvent}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteAccountEmailListener {

    private final DeleteAccountEmail deleteAccountEmail;

    /**
     * Recibe el evento y delega el envío del email OTP de eliminación de cuenta.
     *
     * @param event evento con datos del usuario y el código OTP.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDeleteAccountEmail(DeleteAccountEmailEvent event) {
        log.info("Procesando envío de OTP de eliminación por email para: {}", event.userEmail());
        try {
            deleteAccountEmail.send(event);
        } catch (Exception e) {
            log.error("Error al enviar email OTP de eliminación a {} (ignorado): {}",
                    event.userEmail(), e.getMessage(), e);
        }
    }
}
