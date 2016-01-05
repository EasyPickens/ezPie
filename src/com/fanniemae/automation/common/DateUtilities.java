package com.fanniemae.automation.common;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DateUtilities {
	
	public static String getCurrentDateTime() {
		DateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		return dFormat.format(cal.getTime());
	}
	

}
