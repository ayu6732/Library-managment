package com.library.exceptions;

public class ItemNotFoundException extends Exception  {
	 private static final long serialVersionUID = 1L;
	 /*It's a version control mechanism for Java serialization

When objects are serialized/deserialized, this ID ensures compatibility

Without it, Java generates one automatically, but it can cause issues if the class changes

Adding it explicitly prevents potential serialization issues 
 the compiler recommends adding a serialVersionUID field for better serialization compatibility.*/
	 public ItemNotFoundException(String message) {
	        super(message);
	    }
	    
	 public ItemNotFoundException(String itemId, String itemType) {
	        super(itemType + " with ID '" + itemId + "' not found in the catalog.");
	    }
}
