package com.library.models.persons;

public class Member extends Person {
	 private String position;
	 private int maxLoanLimit;
	 
	 
	 public Member (String id, String name, String email, String phoneNumber, 
             String address, String position ) {
    super(id, name, email, phoneNumber, address);
    this.position = position;
    this.maxLoanLimit = 5; // member can borrow up to 5 items
}
public String getPosition() { 
	return position; 
	}
public void setPosition(String position) {
	this.position = position;
	}

public int getMaxLoanLimit() { 
	return maxLoanLimit; 
	}

@Override
public void displayInfo () {
    super.displayInfo ();
    System.out.println("\n  position" + position + "\n can borrow up to 5 items");
}
}
