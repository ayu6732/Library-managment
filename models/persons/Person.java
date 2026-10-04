package com.library.models.persons;

public abstract class Person {

	    private String id;
	    private String name;
	    private String email;
	    private String phoneNumber;
	    private String address;
	    
	    
	    public Person (String id, String name, String email, String phoneNumber, String address) {
	        this.id = id;
	        this.name = name;
	        this.email = email;
	        this.phoneNumber = phoneNumber;
	        this.address = address;
	    }
	    
	    
	    public String getId() {
			return id;
		}


		public void setId(String id) {
			this.id = id;
		}


		public String getName() {
			return name;
		}


		public void setName(String name) {
			this.name = name;
		}


		public String getEmail() {
			return email;
		}


		public void setEmail(String email) {
			this.email = email;
		}


		public String getPhoneNumber() {
			return phoneNumber;
		}


		public void setPhoneNumber(String phoneNumber) {
			this.phoneNumber = phoneNumber;
		}


		public String getAddress() {
			return address;
		}


		public void setAddress(String address) {
			this.address = address;
		}
		public void displayInfo () {
			System.out.println("Name : " + name + "\n Id : " +id+ "\n Email : " + email + "\n Phone number : " +phoneNumber+ "\n Address : " + address );
		}
		
	}

