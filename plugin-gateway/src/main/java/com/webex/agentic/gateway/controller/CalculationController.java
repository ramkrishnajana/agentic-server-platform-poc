package com.webex.agentic.gateway.controller;

import com.webex.agentic.common.model.CalculationRequest;
import com.webex.agentic.common.model.CalculationResult;
import com.webex.agentic.gateway.service.PluginExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Reactive REST controller for calculation operations using WebFlux
 */
@RestController
@RequestMapping("/api/v1/calculate")
public class CalculationController {
    
    private static final Logger log = LoggerFactory.getLogger(CalculationController.class);

    private final PluginExecutionService executionService;
    
    public CalculationController(PluginExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping("/add")
    public Mono<CalculationResult> add(@RequestBody CalculationRequest request) {
        return executionService.executeCalculation("add_numbers", request)
                .doOnError(e -> log.error("Error executing add operation", e));
    }

    @PostMapping("/multiply")
    public Mono<CalculationResult> multiply(@RequestBody CalculationRequest request) {
        return executionService.executeCalculation("multiply_numbers", request)
                .doOnError(e -> log.error("Error executing multiply operation", e));
    }

    @PostMapping("/subtract")
    public Mono<CalculationResult> subtract(@RequestBody CalculationRequest request) {
        return executionService.executeCalculation("subtract_numbers", request)
                .doOnError(e -> log.error("Error executing subtract operation", e));
    }

    @PostMapping("/divide")
    public Mono<CalculationResult> divide(@RequestBody CalculationRequest request) {
        return executionService.executeCalculation("divide_numbers", request)
                .doOnError(e -> log.error("Error executing divide operation", e));
    }
}

