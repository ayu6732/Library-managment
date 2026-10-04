 package com.library.models.items;

public class Book extends LibraryItem {
	private String ISBN; //International-standard-book-number
	private String author;
	private String publisher;
	private int np; // number of pages
	
	
	public Book(String itemId ,String title , String author, String publisher,int year, String ISBN, int np ) {
		super(itemId, title, year);
		this.ISBN=ISBN;
		this.author = author;
		this.publisher=publisher;
		this.np = np;
	}


	public String getauthor() {
		return author;
	}


	public void setauthor(String author) {
		this.author = author;
	}
	public String getpublisher() {
		return publisher;
	}


	public void setpublisher(String publisher) {
		this.publisher = publisher;
	}

	public int getNp() {
		return np;
	}


	public void setNp(int np) {
		this.np = np;
	}
	public String getISBN() {
		return ISBN;
	}
	
	public void setitemId(String ISBN) {
		this.ISBN = ISBN;
	}
	


	@Override 
	public void displayInfo() {
		System.out.println("Type: Book");
		super.displayInfo();
		System.out.println("ISBN : " + ISBN + "\n The author : " + author +"\n The publisher "  + publisher  + "\n  pages " + np);
	}
	}
