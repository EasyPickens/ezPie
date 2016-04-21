package com.fanniemae.devtools.pie.common;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 
 * @author Richard Monson
 * @since 2016-01-07
 * 
 */
public class DateUtilities {
	
	protected static DateFormat _df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	
	public static String getCurrentDateTime() {
		DateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		return dFormat.format(cal.getTime());
	}
	
	public static String toIsoString(Date value) {
		return (value == null) ? "" : _df.format(value);
	}
	
	public static String toIsoString(Calendar value) {
		DateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		return dFormat.format(value.getTime());
	}

}
