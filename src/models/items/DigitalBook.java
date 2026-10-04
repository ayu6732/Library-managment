package com.library.models.items;

public class DigitalBook extends Book {
	private double fileSize ;//MB 
	private String format ; 
	private String downloadlink ;

	public DigitalBook(String itemId, String title, String author, String publisher, int year, String ISBN, int np,
			double fileSize, String format, String downloadlink) {
		super(itemId, title, author, publisher, year, ISBN, np);
		this.fileSize = fileSize;
		this.format = format;
		this.downloadlink = downloadlink;
	}


	public String getFormat() {
		return format;
	}


	public void setFormat(String format) {
		this.format = format;
	}


	public double getFileSize() {
		return fileSize;
	}


	public void setFileSize(double fileSize) {
		this.fileSize = fileSize;
	}


	public String getDownloadlink() {
		return downloadlink;
	}


	public void setDownloadlink(String downloadlink) {
		this.downloadlink = downloadlink;
	} 
	
	@Override
	public void displayInfo() {
		System.out.println("Type: e-book");
		super.displayInfo();
		System.out.println("Size : " + fileSize + "MB" + "Format : " + format + "\n Downloadlink : " + downloadlink );
	}
	
}
