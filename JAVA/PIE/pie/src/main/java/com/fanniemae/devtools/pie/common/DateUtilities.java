package com.fanniemae.devtools.pie.common;

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

	protected static SimpleDateFormat _sdfISO = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	protected static SimpleDateFormat _sdfPretty = new SimpleDateFormat("MMMM d, yyyy HH:mm:ss");

	public static String getCurrentDateTime() {
		return _sdfISO.format(Calendar.getInstance().getTime());
	}

	public static String getCurrentDateTimePretty() {
		return _sdfPretty.format(Calendar.getInstance().getTime());
	}

	public static String toIsoString(Date value) {
		return (value == null) ? "" : _sdfISO.format(value);
	}

	public static String toIsoString(Calendar value) {
		return (value == null) ? "" : toIsoString(value.getTime());
	}
	
	public static String elapsedTime(long start) {
		long end = System.currentTimeMillis();
		return elapsedTime(start, end);
	}

	public static String elapsedTime(long start, long end) {
		long elapsed = Math.abs(end - start);
		String elapsedPretty;
		if (elapsed < 60000L) {
			elapsedPretty = DurationFormatUtils.formatDuration(elapsed, "s.S' seconds'");
		} else if (elapsed < 3600000L) {
			elapsedPretty = DurationFormatUtils.formatDuration(elapsed, "m' minutes' s.S' seconds'");
		} else if (elapsed < 86400000L) {
			elapsedPretty = DurationFormatUtils.formatDuration(elapsed, "H' hours' m' minutes' s.S' seconds'");
		} else {
			elapsedPretty = DurationFormatUtils.formatDuration(elapsed, "d' days' H' hours' m' minutes' s.S' seconds'");
		}

		// Make days/hours/minutes singular if just one unit.
		if (elapsedPretty.startsWith("1 days "))
			elapsedPretty = elapsedPretty.replace("1 days ", "1 day ");
		if (elapsedPretty.contains(" 1 hours "))
			elapsedPretty = elapsedPretty.replace(" 1 hours ", " 1 hour ");
		if (elapsedPretty.contains(" 1 minutes "))
			elapsedPretty = elapsedPretty.replace(" 1 minutes ", " 1 minute ");
		if (end - start < 0) {
			return "Negative " + elapsedPretty;
		}
		return elapsedPretty;
	}
	
	public static String elapsedTimeShort(long start) {
		long end = System.currentTimeMillis();
		return elapsedTimeShort(start, end);
	}

	public static String elapsedTimeShort(long start, long end) {
		long elapsed = Math.abs(end - start);
		String elapsedPretty;
		if (elapsed < 60000L) {
			elapsedPretty = DurationFormatUtils.formatDuration(elapsed, "s.S's'");
		} else if (elapsed < 3600000L) {
			elapsedPretty = DurationFormatUtils.formatDuration(elapsed, "m'm' s.S's'");
		} else if (elapsed < 86400000L) {
			elapsedPretty = DurationFormatUtils.formatDuration(elapsed, "H'h' m'm' s.S's'");
		} else {
			elapsedPretty = DurationFormatUtils.formatDuration(elapsed, "d'd' H'h' m'm' s.S's'");
		}
		if (end - start < 0) {
			return "Negative " + elapsedPretty;
		}		
		return elapsedPretty;
	}

}
