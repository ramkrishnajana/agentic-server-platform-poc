package com.webex.agentic.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalculationResult {
    private double result;
    private String operation;
    private double operand1;
    private double operand2;
}

