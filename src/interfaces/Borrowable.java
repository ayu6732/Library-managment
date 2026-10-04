package com.library.interfaces;

public interface Borrowable {
	
	// true if item is available for borrowing  false if already borrowed
	
	    boolean canBeBorrowed();
	    
	    void markAsBorrowed();
	    
	    void markAsReturned();
	}
