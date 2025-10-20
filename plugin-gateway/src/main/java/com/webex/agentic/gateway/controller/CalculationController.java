package com.webex.agentic.gateway.controller;

import com.webex.agentic.common.model.CalculationRequest;
import com.webex.agentic.common.model.CalculationResult;
import com.webex.agentic.gateway.service.PluginExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for calculation operations
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
    public ResponseEntity<CalculationResult> add(@RequestBody CalculationRequest request) {
        try {
            CalculationResult result = executionService.executeCalculation("add_numbers", request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error executing add operation", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/multiply")
    public ResponseEntity<CalculationResult> multiply(@RequestBody CalculationRequest request) {
        try {
            CalculationResult result = executionService.executeCalculation("multiply_numbers", request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error executing multiply operation", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/subtract")
    public ResponseEntity<CalculationResult> subtract(@RequestBody CalculationRequest request) {
        try {
            CalculationResult result = executionService.executeCalculation("subtract_numbers", request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error executing subtract operation", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

