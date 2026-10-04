package com.library.models.transactions;
import com.library.models.persons.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.library.models.items.LibraryItem;


public class Reservation extends Transaction {
   public  LocalDate expirationDate;
   public boolean isNotified;
    
    //reservation rules
    public static final int RESERVATION_HOLD_DAYS = 3; // Hold item for 3 days
    public static final double CANCELLATION_FEE = 10.0; // Fee if not picked up
    
   
    public Reservation(String transactionId, Person person, LibraryItem item) {
        super(transactionId, person, item);
        this.expirationDate = LocalDate.now().plusDays(RESERVATION_HOLD_DAYS);
        this.isNotified = false;
    }
    
    public LocalDate getExpirationDate() {
        return expirationDate;
    }
    
    public boolean isNotified() {
        return isNotified;
    }
    
    public void setNotified(boolean notified) {
        this.isNotified = notified;
    }
    
    
    @Override
    public boolean isOverdue() {
        if (status.equals("COMPLETED") || status.equals("CANCELLED")) {
            return false;
        }
        return LocalDate.now().isAfter(expirationDate);
    }
    
    
    @Override
    public double calculateFees() {
        if (isOverdue() && status.equals("ACTIVE")) {
            return CANCELLATION_FEE;
        }
        return 0.0;
    }
    
    
    @Override
    public void complete() {
        if (status.equals("COMPLETED")) {
            System.out.println("This reservation has already been completed.");
            return;
        }
        
        if (isOverdue()) {
            System.out.println("Reservation expired. Cannot complete.");
            double fee = calculateFees();
            System.out.println("Cancellation fee charged: " + String.format("%.2f", fee)+ "DA");
            cancel();
            return;
        }
        
        this.status = "COMPLETED";
        System.out.println("\n Reservation Completed " +
        		"\n Item picked up: " + item.getTitle()+
        		"\n Person: " + person.getName() +
        		"\n Picked up on: " + LocalDate.now());
       
    }
    
    // Notify person that item is available
    public void notifyPerson() {
        if (!isNotified) {
            this.isNotified = true;
            System.out.println("\n NOTIFICATION SENT" + 
            		"\n To: " + person.getEmail()+
            		"\n Recipient: " + person.getName()+
            		"\n Subject: Reserved Item Available" +
            		"\n Message: Your reserved item '" + item.getTitle() + 
                    " is now available. Please pick it up by " + expirationDate);
        } else {
            System.out.println("Notification already sent to " + person.getName());
        }
    }
    
    public void displayInfo() {
        displayBasicInfo();
        System.out.println("\n Expiration Date: " + expirationDate +
        		"\n Notified: " + (isNotified ? "Yes" : "No"));
       
        if (isOverdue() && status.equals("ACTIVE")) {
            System.out.println("\n EXPIRED ! Cancellation fee: " + 
                             String.format("%.2f", calculateFees())+ "DA");
        } else if (status.equals("ACTIVE")) {
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), expirationDate);
            System.out.println("Days left to pick up: " + daysLeft);
        }
       
    }
}


