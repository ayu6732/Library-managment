package com.library.models.persons;


	public class Student extends Person {
	    private String department;
	    private int maxLoanLimit;
	    
	    public Student(String id, String name, String email, String phoneNumber, String address,int maxLoanLimit, String department) {
	        super(id, name, email, phoneNumber, address);
	        this.department = department;
	        this.maxLoanLimit = 5; 
	    }
	    
	    
	    public String getDepartment() {
			return department;
		}


		public void setDepartment(String department) {
			this.department = department;
		}


		public int getMaxLoanLimit() {
			return maxLoanLimit;
		}


		public void setMaxLoanLimit(int maxLoanLimit) {
			this.maxLoanLimit = maxLoanLimit;
		}
		
		
		@Override
		public void displayInfo() {
			System.out.println("Student of :" + department + "\n");
			super.displayInfo();
			
		}

		
	}
