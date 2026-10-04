package com.library.models.persons;

	public class Professor extends Person {
	    private String department;
	    private String speciality;
	    private int maxLoanLimit;
	    
	    public Professor(String id, String name, String email, String phoneNumber, String address, String department, String speciality) {
	        super(id, name, email, phoneNumber, address);
	        this.department = department;
	        this.speciality = speciality;
	        this.maxLoanLimit = 10; 
	    }
	    
	   


		public String getDepartment() {
			return department;
		}



		public void setDepartment(String department) {
			this.department = department;
		}



		public String getSpeciality() {
			return speciality;
		}



		public void setSpeciality(String speciality) {
			this.speciality = speciality;
		}



		public int getMaxLoanLimit() {
			return maxLoanLimit;
		}



		public void setMaxLoanLimit(int maxLoanLimit) {
			this.maxLoanLimit = maxLoanLimit;
		}



		@Override
	    public void displayInfo() {
		System.out.println("Professor of :" + department + speciality +"\n");
		super.displayInfo();
	    }
	}

