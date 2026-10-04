package com.library.interfaces;

	 //Applied to: Items that are currently borrowed but can be reserved

	public interface Reservable {
		
		boolean canBeReserved();//Usually true when item is currently borrowed
	
		boolean reserve(String memberId);
	
		boolean cancelReservation(String memberId);
	
	}
