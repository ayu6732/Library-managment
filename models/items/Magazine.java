package com.library.models.items;

public class Magazine extends LibraryItem {
	 private String publisher ;
	 private String isDate; // issue date
	 private int vn ; // volume number
	  
	
	
	public Magazine(String itemId, String title, int year, String publisher, String isDate, int vn) {
		super(itemId, title, year);
		this.publisher = publisher;
		this.isDate = isDate;
		this.vn = vn;
	}
	

	public String getIsDate() {
		return isDate;
	}


	public void setIsDate(String isDate) {
		this.isDate = isDate;
	}


	public String getPublisher() {
		return publisher;
	}


	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}


	public String getIssueDate() {
		return isDate;
	}
	
	public void setIssueDate(String isDate) {
		this.isDate = isDate;
	}
	
	public int getVn() {
		return vn;
	}
	
	public void setVn(int vn) {
		this.vn = vn;
	}
	@Override
	public void displayInfo() {
		System.out.println("Type: Magazine");
		super.displayInfo();
		System.out.println( "Volume : " + vn + "\n Issue Date : " + isDate + "\n Published by " + publisher );
	}
  
}
