package com.library.exceptions;

public class LateFeeException extends Exception{
	 private static final long serialVersionUID = 1L;
	 private double feeAmount;
    
    public LateFeeException(String message) {
        super(message);
    }
    
    public LateFeeException(String message, double feeAmount) {
        super(message + " Fee amount: " + String.format("%.2f", feeAmount) + " DA");
        this.feeAmount = feeAmount;
    }
    
    public double getFeeAmount() {
        return feeAmount;
    }
}
