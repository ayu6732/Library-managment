package com.library.models.transactions;
import com.library.models.persons.*;
import com.library.models.items.LibraryItem;

import java.time.LocalDate;


public abstract class Transaction {
    protected String transactionId;
    protected Person person; 
    protected LibraryItem item;
    protected LocalDate transactionDate;
    protected String status; // ACTIVE, COMPLETED, CANCELLED
    
    
    public Transaction(String transactionId, Person person, LibraryItem item) {
        this.transactionId = transactionId;
        this.person = person;
        this.item = item;
        this.transactionDate = LocalDate.now();
        this.status = "ACTIVE";
    }
    
    public abstract double calculateFees();
    
    public abstract void complete();
    
    public abstract boolean isOverdue();
    
    public String getTransactionId() { 
        return transactionId; 
    }
    
    public Person getPerson() { 
        return person; 
    }
    
    public LibraryItem getItem() { 
        return item; 
    }
    
    public LocalDate getTransactionDate() { 
        return transactionDate; 
    }
    
    public String getStatus() { 
        return status; 
    }
    
    //instanceof check persons type
    protected int getMaxLoanLimit() {
        if (person instanceof Professor) {
            return ((Professor) person).getMaxLoanLimit(); // 10
        } else if (person instanceof Staff) {
            return ((Staff) person).getMaxLoanLimit(); // 7
        } else if (person instanceof Student) {
            return ((Student) person).getMaxLoanLimit(); // 5
        } else if (person instanceof Member) {
            return ((Member) person).getMaxLoanLimit(); // 5
        }
        return 5; // Default
    }
    
    public void cancel() {
        this.status = "CANCELLED";
        System.out.println("Transaction " + transactionId + " has been cancelled.\n ");
    }
    
  
    public void displayBasicInfo() {
        // Check person type using instanceof
        String personType = "";
        int maxLoanLimit = 5; // default
        String additionalInfo = "";
        if (person instanceof Professor) {
            personType = "Professor";
            Professor prof = (Professor) person;
            maxLoanLimit = prof.getMaxLoanLimit();
            additionalInfo = "Department: " + prof.getDepartment() + 
                           ", Speciality: " + prof.getSpeciality();
        } else if (person instanceof Student) {
            personType = "Student";
            Student student = (Student) person;
            maxLoanLimit = student.getMaxLoanLimit();
            additionalInfo = "Department: " + student.getDepartment();
        } else if (person instanceof Staff) {
            personType = "Staff";
            Staff staff = (Staff) person;
            maxLoanLimit = staff.getMaxLoanLimit();
            additionalInfo = "Position: " + staff.getPosition();
        } else if (person instanceof Member) {
            personType = "Member";
            Member member = (Member) person;
            maxLoanLimit = member.getMaxLoanLimit();
            additionalInfo = "Position: " + member.getPosition();
        }
        System.out.println("Transaction ID: " + transactionId + 
        		"\n the"+ personType + person.getName()
        		+ "\n ID: " + person.getId()
        		+ "\n Email: " + person.getEmail()
        		+"\n Max Loan Limit: " + maxLoanLimit + " items \n ");
        if (!additionalInfo.isEmpty()) {
            System.out.println(additionalInfo);
        } 
        
        System.out.println("Item: " + item.getTitle()
        + " Item ID : " + item.getItemId()
        + "  Transaction Date: " + transactionDate
       + "  Status: " + status);
        
    }
}

