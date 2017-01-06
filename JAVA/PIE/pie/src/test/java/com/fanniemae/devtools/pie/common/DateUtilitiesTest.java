package com.fanniemae.devtools.pie.common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.junit.Test;

import junit.framework.TestCase;

public class DateUtilitiesTest extends TestCase {

	protected static final String DATE_VALUE = "2016-03-05T13:34:01";
	protected static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	protected static final SimpleDateFormat SIMPLE_DATE_FORMAT_MILLS = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

	private static Date getDateObject(String value, boolean hasMills) {
		try {
			if (hasMills) {
				return SIMPLE_DATE_FORMAT_MILLS.parse(value);
			} else {
			return SIMPLE_DATE_FORMAT.parse(value);
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static Calendar getCalendarObject(String value, boolean hasMills) {
		try {
			Calendar calendar = Calendar.getInstance();
			if (hasMills) {
			calendar.setTime(SIMPLE_DATE_FORMAT_MILLS.parse(value));
			} else {
				calendar.setTime(SIMPLE_DATE_FORMAT.parse(value));
			}
			return calendar;
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Test
	public void testToIsoStringWithDate() {
		Date date = getDateObject(DATE_VALUE, false);
		assertEquals(DATE_VALUE, DateUtilities.toIsoString(date));
	}

	@Test
	public void testToIsoStringWithNullDate() {
		Date date = null;
		assertEquals("", DateUtilities.toIsoString(date));
	}

	@Test
	public void testToIsoStringWithCalendar() {
		Calendar calendar = getCalendarObject(DATE_VALUE, false);
		assertEquals(DATE_VALUE, DateUtilities.toIsoString(calendar));
	}

	@Test
	public void testToIsoStringWithNullCalendar() {
		Calendar calendar = null;
		assertEquals("", DateUtilities.toIsoString(calendar));
	}
	
	//***********************************************************************************
	//* Test cases for full format elapsed time strings 
	//***********************************************************************************
	
	@Test
	public void testElapsedTimeMills() {
		long start = getCalendarObject("2016-03-05T13:34:01.000", true).getTimeInMillis();
		long end = getCalendarObject("2016-03-05T13:34:01.130", true).getTimeInMillis();
		assertEquals("0.130 seconds", DateUtilities.elapsedTime(start, end));
	}
	
	@Test
	public void testElapsedTimeSeconds() {
		long start = getCalendarObject("2016-03-05T13:34:01.000", true).getTimeInMillis();
		long end = getCalendarObject("2016-03-05T13:34:23.130", true).getTimeInMillis();
		assertEquals("22.130 seconds", DateUtilities.elapsedTime(start, end));
	}
	
	@Test
	public void testElapsedTimeMinutes() {
		long start = getCalendarObject("2016-03-05T13:34:01.000", true).getTimeInMillis();
		long end = getCalendarObject("2016-03-05T13:37:23.435", true).getTimeInMillis();
		assertEquals("3 minutes 22.435 seconds", DateUtilities.elapsedTime(start, end));
	}
	
	@Test
	public void testElapsedTimeHours() {
		long start = getCalendarObject("2016-03-05T13:34:01.000", true).getTimeInMillis();
		long end = getCalendarObject("2016-03-05T18:37:23.435", true).getTimeInMillis();
		assertEquals("5 hours 3 minutes 22.435 seconds", DateUtilities.elapsedTime(start, end));
	}
	
	@Test
	public void testElapsedTimeDays() {
		long start = getCalendarObject("2016-03-05T13:34:01.000", true).getTimeInMillis();
		long end = getCalendarObject("2016-03-09T18:37:23.435", true).getTimeInMillis();
		assertEquals("4 days 5 hours 3 minutes 22.435 seconds", DateUtilities.elapsedTime(start, end));
	}
	
	@Test
	public void testElapsedTimeZero() {
		long start = getCalendarObject(DATE_VALUE, false).getTimeInMillis();
		long end = getCalendarObject(DATE_VALUE, false).getTimeInMillis();
		assertEquals("0.000 seconds", DateUtilities.elapsedTime(start, end));
	}
	
	@Test
	public void testElapsedTimeNegative() {
		long start = getCalendarObject("2016-03-05T13:34:01.000", true).getTimeInMillis();
		long end = getCalendarObject("2016-03-05T10:30:23.435", true).getTimeInMillis();
		assertEquals("Negative 3 hours 3 minutes 37.565 seconds", DateUtilities.elapsedTime(start, end));
	}

	//***********************************************************************************
	//* Test cases for short format elapsed time strings 
	//***********************************************************************************
	
	@Test
	public void testShortElapsedTimeMills() {
		long start = getCalendarObject("2016-03-05T13:34:01.000", true).getTimeInMillis();
		long end = getCalendarObject("2016-03-05T13:34:01.130", true).getTimeInMillis();
		assertEquals("0.130s", DateUtilities.elapsedTimeShort(start, end));
	}
	
	@Test
	public void testShortElapsedTimeSeconds() {
		long start = getCalendarObject("2016-03-05T13:34:01.000", true).getTimeInMillis();
		long end = getCalendarObject("2016-03-05T13:34:23.130", true).getTimeInMillis();
		assertEquals("22.130s", DateUtilities.elapsedTimeShort(start, end));
	}
	
	@Test
	public void testShortElapsedTimeMinutes() {
		long start = getCalendarObject("2016-03-05T13:34:01.000", true).getTimeInMillis();
		long end = getCalendarObject("2016-03-05T13:37:23.435", true).getTimeInMillis();
		assertEquals("3m 22.435s", DateUtilities.elapsedTimeShort(start, end));
	}
	
	@Test
	public void testShortElapsedTimeHours() {
		long start = getCalendarObject("2016-03-05T13:34:01.000", true).getTimeInMillis();
		long end = getCalendarObject("2016-03-05T18:37:23.435", true).getTimeInMillis();
		assertEquals("5h 3m 22.435s", DateUtilities.elapsedTimeShort(start, end));
	}
	
	@Test
	public void testShortElapsedTimeDays() {
		long start = getCalendarObject("2016-03-05T13:34:01.000", true).getTimeInMillis();
		long end = getCalendarObject("2016-03-09T18:37:23.435", true).getTimeInMillis();
		assertEquals("4d 5h 3m 22.435s", DateUtilities.elapsedTimeShort(start, end));
	}
	
	@Test
	public void testShortElapsedTimeZero() {
		long start = getCalendarObject(DATE_VALUE, false).getTimeInMillis();
		long end = getCalendarObject(DATE_VALUE, false).getTimeInMillis();
		assertEquals("0.000s", DateUtilities.elapsedTimeShort(start, end));
	}
	
	@Test
	public void testShortElapsedTimeNegative() {
		long start = getCalendarObject("2016-03-05T13:34:01.000", true).getTimeInMillis();
		long end = getCalendarObject("2016-03-05T10:30:23.435", true).getTimeInMillis();
		assertEquals("Negative 3h 3m 37.565s", DateUtilities.elapsedTimeShort(start, end));
	}
}
