package com.webex.agentic.common.model;

public class CalculationRequest {
    private double operand1;
    private double operand2;
    
    public CalculationRequest() {
    }
    
    public CalculationRequest(double operand1, double operand2) {
        this.operand1 = operand1;
        this.operand2 = operand2;
    }
    
    public double getOperand1() {
        return operand1;
    }
    
    public void setOperand1(double operand1) {
        this.operand1 = operand1;
    }
    
    public double getOperand2() {
        return operand2;
    }
    
    public void setOperand2(double operand2) {
        this.operand2 = operand2;
    }
}

