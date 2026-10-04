package com.library.exceptions;

public class InvalidISBNException extends Exception {
	    private static final long serialVersionUID = 1L;
	    private String isbn;
	    
	    // Constructor for custom error messages (isbn can be null)
	    public InvalidISBNException(String message) {
	        super(message);
	        this.isbn = null; // No specific ISBN
	    }
	    
	    // Constructor that stores the invalid ISBN
	    public InvalidISBNException(String isbn, boolean storeISBN) {
	        super("Invalid ISBN format: " + isbn + ". ISBN must be 10 or 13 digits.");
	        this.isbn = storeISBN ? isbn : null;
	    }
	    
	    
	    public InvalidISBNException(String isbn, String reason) {
	        super("Invalid ISBN '" + isbn + "': " + reason);
	        this.isbn = isbn;
	    }
	    
	    public String getIsbn() {
	        return isbn;
	    }
	    
	    // ISBN validation helper
	    public static boolean isValidISBN(String isbn) {
	        if (isbn == null) return false;
	        
	        // Remove hyphens and spaces
	        String cleanISBN = isbn.replaceAll("[-\\s]", "");
	        
	        // Check if it's 10 or 13 digits
	        return cleanISBN.matches("\\d{10}") || cleanISBN.matches("\\d{13}");
	    }
	}
