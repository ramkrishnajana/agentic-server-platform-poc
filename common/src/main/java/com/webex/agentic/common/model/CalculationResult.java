package com.webex.agentic.common.model;

public class CalculationResult {
    private double result;
    private String operation;
    private double operand1;
    private double operand2;
    
    public CalculationResult() {
    }
    
    public CalculationResult(double result, String operation, double operand1, double operand2) {
        this.result = result;
        this.operation = operation;
        this.operand1 = operand1;
        this.operand2 = operand2;
    }
    
    public double getResult() {
        return result;
    }
    
    public void setResult(double result) {
        this.result = result;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public void setOperation(String operation) {
        this.operation = operation;
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

