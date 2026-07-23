package com.sportsbooking.mainapi.scheduler;

import com.sportsbooking.mainapi.service.VenueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scheduler.venue-disable.enabled", havingValue = "true", matchIfMissing = true)
public class VenueDeadlineScheduler {

    private final VenueService venueService;

    @Scheduled(fixedDelayString = "${app.scheduler.venue-disable.interval:3600}", timeUnit = TimeUnit.SECONDS)
    public void disableVenuesPastDeadline() {
        log.debug("[VenueDeadlineScheduler] Iniciando tarea programada para inhabilitar salones con deudas vencidas...");
        try {
            venueService.disableVenuesPastDeadline();
        } catch (Exception e) {
            log.error("[VenueDeadlineScheduler] Error al ejecutar la inhabilitación automática de salones: ", e);
        }
    }
}
