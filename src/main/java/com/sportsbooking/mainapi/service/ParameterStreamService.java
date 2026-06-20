package com.sportsbooking.mainapi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbooking.mainapi.jpa.entity.ParameterEntity;
import com.sportsbooking.mainapi.jpa.repository.ParameterRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
// package com.sportsbooking.mainapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ParameterStreamService {

    private static final Logger log = LoggerFactory.getLogger(ParameterStreamService.class);

    private final ParameterRepository parameterRepository;
    private final ObjectMapper objectMapper;

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private volatile String lastSnapshot = "";

    public ParameterStreamService(ParameterRepository parameterRepository, ObjectMapper objectMapper) {
        this.parameterRepository = parameterRepository;
        this.objectMapper = objectMapper;
    }

    public SseEmitter registerEmitter() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));
        // send initial data
        try {
            String snapshot = objectMapper.writeValueAsString(parameterRepository.findAll());
            emitter.send(SseEmitter.event().name("parameters").data(snapshot));
            log.info("Registered SSE emitter, current emitters={}", emitters.size());
        } catch (Exception ignored) {
        }
        return emitter;
    }

    // Método invocado periódicamente por el scheduler para detectar cambios.
    @org.springframework.scheduling.annotation.Scheduled(fixedDelayString = "5000")
    public void pollAndEmit() {
        List<ParameterEntity> list = parameterRepository.findAll();
        try {
            String snapshot = objectMapper.writeValueAsString(list);
            if (!snapshot.equals(lastSnapshot)) {
                lastSnapshot = snapshot;
                log.info("Detected parameter changes, broadcasting to {} emitters", emitters.size());
                for (SseEmitter emitter : emitters) {
                    try {
                        emitter.send(SseEmitter.event().name("parameters").data(snapshot));
                    } catch (Exception ex) {
                        emitters.remove(emitter);
                    }
                }
            }
        } catch (JsonProcessingException e) {
            // ignorar errores de serialización por ahora
            log.error("Error serializando parámetros: {}", e.getMessage(), e);
        }
    }
}

// String snapshot = objectMapper.writeValueAsString(list);
