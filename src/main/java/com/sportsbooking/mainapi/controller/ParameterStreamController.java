package com.sportsbooking.mainapi.controller;

import com.sportsbooking.mainapi.jpa.entity.ParameterEntity;
import com.sportsbooking.mainapi.jpa.repository.ParameterRepository;
import com.sportsbooking.mainapi.service.ParameterStreamService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/parameters")
public class ParameterStreamController {

    private final ParameterStreamService streamService;
    private final ParameterRepository parameterRepository;

    public ParameterStreamController(ParameterStreamService streamService, ParameterRepository parameterRepository) {
        this.streamService = streamService;
        this.parameterRepository = parameterRepository;
    }

    @GetMapping("/stream")
    public SseEmitter stream() {
        return streamService.registerEmitter();
    }

    @GetMapping
    public List<ParameterEntity> getAll() {
        return parameterRepository.findAll();
    }
}
