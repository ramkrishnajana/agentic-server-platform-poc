package com.webex.agentic.gateway.controller;

import com.webex.agentic.common.model.CalculationRequest;
import com.webex.agentic.common.model.CalculationResult;
import com.webex.agentic.gateway.service.PluginExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for calculation operations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/calculate")
@RequiredArgsConstructor
public class CalculationController {

    private final PluginExecutionService executionService;

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

