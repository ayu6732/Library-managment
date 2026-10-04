package com.library.exceptions;

	public class ItemNotAvailableException extends Exception {
		private static final long serialVersionUID = 1L;
	    private String itemId;
	    private String itemTitle;
	    
	    public ItemNotAvailableException(String message) {
	        super(message);
	    }
	    
	    public ItemNotAvailableException(String itemId, String itemTitle) {
	        super("Item '" + itemTitle + "' (ID: " + itemId + ") is not available for borrowing.");
	        this.itemId = itemId;
	        this.itemTitle = itemTitle;
	    }
	    
	    public String getItemId() {
	        return itemId;
	    }
	    
	    public String getItemTitle() {
	        return itemTitle;
	    }
	}
