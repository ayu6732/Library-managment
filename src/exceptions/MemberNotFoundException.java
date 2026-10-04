package com.library.exceptions;

public class MemberNotFoundException extends Exception {
    private static final long serialVersionUID = 1L;
    
    // General message constructor
    public MemberNotFoundException(String message) {
        super(message);
    }
    
    // Overloaded constructor for member ID
    public MemberNotFoundException(String PersonId, String context) {
        super("Member with ID " + PersonId + "' not found" + 
              (context != null ? " in " + context : " in the system") + ".");
    }
    
}