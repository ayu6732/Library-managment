package com.library.models.items;
import com.library.interfaces.Borrowable;
import com.library.interfaces.Reservable;
import java.util.ArrayList;
import java.util.List;
public abstract class LibraryItem implements Borrowable , Reservable {
	private String itemId;
	private String title;
	private int year ;
	private boolean borrowed;
	private List<String> reservationQueue; // Store member IDs who reserved
	
	
	public LibraryItem(String itemId , String title,int year) {
		this.itemId=itemId;
		this.title = title;
		this.year=year;
		this.borrowed = false;
	    this.reservationQueue = new ArrayList<>();
	}
	
	
	public String getItemId() {
		return itemId;
	}


	public void setItemId(String itemId) {
		this.itemId = itemId;
	}

	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	public int getYear() {
		return year;
	}


	public void setYear(int year) {
		this.year = year;
	}
	public boolean isAvailable() {
        return !borrowed;
    }

	 public String getAvailabilityStatus() {
	        if (borrowed) {
	            return "Not Available (Borrowed)";
	        } else if (!reservationQueue.isEmpty()) {
	            return "Available (Reserved by " + reservationQueue.size() + " people)";
	        } else {
	            return "Available";
	        }
	    }
	    
	  
	    @Override
	    public boolean canBeBorrowed() {
	        return !borrowed; // Can borrow if not already borrowed
	    }
	    
	    @Override
	    public void markAsBorrowed() {
	        if (borrowed) {
	            System.out.println("⚠️  Item is already borrowed!");
	            return;
	        }
	        this.borrowed = true;
	       //System.out.println("✓ Item marked as borrowed: " + title);
	    }
	    
	    @Override
	    public void markAsReturned() {
	        if (!borrowed) {
	            System.out.println("⚠️  Item was not borrowed!");
	            return;
	        }
	        this.borrowed = false;
	      //  System.out.println("✓ Item marked as returned: " + title);
	        
	        
	        if (!reservationQueue.isEmpty()) {
	            System.out.println("📢 Notifying next person in reservation queue...");
	        }
	    }
	    
	   
	    
	    @Override
	    public boolean canBeReserved() {
	        // Can be reserved if it's currently borrowed
	        return borrowed;
	    }
	    
	    @Override
	    public boolean reserve(String PersonId) {
	        if (!canBeReserved()) {
	            System.out.println("✗ Item is available. No need to reserve: " + title);
	            return false;
	        }
	        
	        // Check if member already reserved
	        if (reservationQueue.contains(PersonId)) {
	            System.out.println("✗ Member already has a reservation for: " + title);
	            return false;
	        }
	        
	        reservationQueue.add(PersonId);
	        System.out.println("✓ Reservation added for member: " + PersonId);
	        System.out.println("  Queue position: " + reservationQueue.size());
	        return true;
	    }
	    
	    @Override
	    public boolean cancelReservation(String PersonId) {
	        if (reservationQueue.remove(PersonId)) {
	            System.out.println("✓ Reservation cancelled for member: " + PersonId);
	            return true;
	        } else {
	            System.out.println("✗ No reservation found for member: " + PersonId);
	            return false;
	        }
	    }
	    
	    
	    public List<String> getReservationQueue() {
	        return new ArrayList<>(reservationQueue);
	    }
	    
	    public int getReservationCount() {
	        return reservationQueue.size();
	    }
	    
	 
	    public void displayInfo() {
	        System.out.println("Item ID: " + itemId
	        		+"\n Title: " + title +
	        		"\n Year: " + year
	        	+"\n Status: " + getAvailabilityStatus());
	        
	        if (!reservationQueue.isEmpty()) {
	            System.out.println("Reservations: " + reservationQueue.size() + " people waiting");
	        }
	    }
	}
