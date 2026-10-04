package com.library.models.transactions;
import com.library.models.persons.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import com.library.models.items.LibraryItem;


public class Loan extends Transaction {
	
	    private LocalDate dueDate;
	    private LocalDate returnDate;
	    private double lateFee;
	    
	    private static final int LOAN_PERIOD_DAYS = 14; // 2 weeks
	    private static final double LATE_FEE_PER_DAY = 50; // 50 DA per day
	    
	   
	    public Loan(String transactionId, Person person, LibraryItem item) {
	        super(transactionId, person, item); 
	        this.dueDate = LocalDate.now().plusDays(LOAN_PERIOD_DAYS);
	        this.returnDate = null;
	        this.lateFee = 0.0;
	    }
	    
	    
	    public Loan(String transactionId, Person person, LibraryItem item, int loanPeriodDays) {
	        super(transactionId, person, item);
	        this.dueDate = LocalDate.now().plusDays(loanPeriodDays);
	        this.returnDate = null;
	        this.lateFee = 0.0;
	    }
	    
	   
	    public LocalDate getDueDate() {
	        return dueDate;
	    }
	    
	    public void setDueDate(LocalDate dueDate) {
	        this.dueDate = dueDate;
	    }
	    
	    public LocalDate getReturnDate() {
	        return returnDate;
	    }
	    
	    public double getLateFee() {
	        return lateFee;
	    }
	
	    public void setStatus(String status) {
	        this.status = status;
	    }
	    public void setReturnDate(LocalDate returnDate) {
	        this.returnDate = returnDate;
	    }
	    // loan is overdue?
	    @Override
	    public boolean isOverdue() {
	        if (returnDate != null) {
	            return false; //returned
	        }
	        return LocalDate.now().isAfter(dueDate);
	    }
	    
	    // Calculate late fees
	    @Override
	    public double calculateFees() {
	        if (!isOverdue()) {
	            return 0.0;
	        }
	        
	        long daysOverdue = ChronoUnit.DAYS.between(dueDate, LocalDate.now());
	        lateFee = daysOverdue * LATE_FEE_PER_DAY;
	        return lateFee;
	    }
	    
	    // Complete the loan (return item)
	    @Override
	    public void complete() {
	        if (status.equals("COMPLETED")) {
	            System.out.println("This loan has already been completed.\n ");
	            return;
	        }
	        
	        this.returnDate = LocalDate.now();
	        this.status = "COMPLETED";
	        
	        //late fees
	        double fees = calculateFees();
	        
	        System.out.println("\n Loan Completed \n" 
	        +"Item returned: " + item.getTitle()
	        +"Returned by: " + person.getName()
	        +"Due Date: " + dueDate);
	        
	        if (fees > 0) {
	            System.out.println("\n OVERDUE! Late fee:" + String.format("%.2f", fees) + "DA");
	        } else {
	            System.out.println("\n Returned on time. No fees.");
	        }
	        
	    }
	  
	    public void renewLoan(int additionalDays) {
	        if (status.equals("COMPLETED")) {
	            System.out.println("\n Cannot renew a completed loan !!.");
	            return;
	        }
	        
	        if (isOverdue()) {
	            System.out.println("\n Cannot renew an overdue loan. Please return the item.");
	            return;
	        }
	        
	        this.dueDate = this.dueDate.plusDays(additionalDays);
	        System.out.println("Loan renewed! New due date: " + dueDate);
	    }
	    
	   
	    public void displayInfo() {
	        super.displayBasicInfo();
	        System.out.println("Due Date: " + dueDate);
	        if (returnDate != null) {
	            System.out.println("Return Date: " + returnDate);
	        } else {
	            System.out.println("Return Date: Not yet returned");
	        }
	        
	        if (isOverdue() && returnDate == null) {
	            long daysOverdue = ChronoUnit.DAYS.between(dueDate, LocalDate.now());
	            System.out.println("OVERDUE by " + daysOverdue + " days!");
	            System.out.println("Late Fee: " + String.format("%.2f", calculateFees())+"DA");
	        }
	       
	        System.out.println("Max Loan Limit: " + getMaxLoanLimit() + " items");
	
	    }
	}
