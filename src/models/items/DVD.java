package com.library.models.items;

public class DVD extends LibraryItem{
	private String director ;
	private int duration ; // minutes
	private String rating;
	
	public DVD(String itemId, String title, int year, String director, int duration, String rating) {
		super(itemId, title, year);
		this.director = director;
		this.duration = duration;
		this.rating = rating;
	}

	public String getDirector() {
		return director;
	}

	public void setDirector(String director) {
		this.director = director;
	}

	public int getDuration() {
		return duration;
	}
	
	public void setDuration(int duration) {
		this.duration = duration;
	}
	
	public String getRating() {
		return rating;
	}
	
	public void setRating(String rating) {
		this.rating = rating;
	} 
	
	
	@Override 
	public void displayInfo() {
		System.out.println("Type: DVD");
		super.displayInfo();
		System.out.println("Director :" + director +"\n Duration :" + duration +" min , " + "\n Rating : " + rating);
	}
}
