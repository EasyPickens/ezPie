package com.fanniemae.devtools.pie.common;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.time.DurationFormatUtils;

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
	
	public static String elapsedTime(long start) {
		String elapsed = DurationFormatUtils.formatDuration(System.currentTimeMillis() - start, "d' days' H' hours' m' minutes' s.S' seconds'");
		// Make days/hours/minutes singular if just one unit.
		if (elapsed.startsWith("1 days "))
			elapsed = elapsed.replace("1 days ", "1 day ");
		if (elapsed.contains(" 1 hours "))
			elapsed = elapsed.replace(" 1 hours ", " 1 hour ");
		if (elapsed.contains(" 1 minutes "))
			elapsed = elapsed.replace(" 1 minutes ", " 1 minute ");

		// Remove the 0 periods if not needed.
		if (elapsed.startsWith("0 days "))
			elapsed = elapsed.replace("0 days ", "");
		if (elapsed.startsWith("0 hours "))
			elapsed = elapsed.replace("0 hours ", "");
		if (elapsed.startsWith("0 minutes "))
			elapsed = elapsed.replace("0 minutes ", "");
		return elapsed;
	}

}
