package com.library.exceptions;

public class LoanLimitExceededException extends Exception {
	private static final long serialVersionUID = 1L;
	private int currentLoans;
    private int maxLimit;
    
    public LoanLimitExceededException(String message) {
        super(message);
    }
    
    public LoanLimitExceededException(String PersonName, int currentLoans, int maxLimit) {
        super("Loan limit exceeded for " + PersonName + ". Current loans: " + 
              currentLoans + "/" + maxLimit);
        this.currentLoans = currentLoans;
        this.maxLimit = maxLimit;
    }
    
    public int getCurrentLoans() {
        return currentLoans;
    }
    
    public int getMaxLimit() {
        return maxLimit;
    }
}
