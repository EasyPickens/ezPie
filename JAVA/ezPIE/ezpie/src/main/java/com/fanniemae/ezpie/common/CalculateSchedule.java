/**
 *  
 * Copyright (c) 2017 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 *
 */

package com.fanniemae.ezpie.common;

import java.time.LocalDateTime;

import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.SessionManager;

/**
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2017-06-04
 *
 */

public class CalculateSchedule {
	private SessionManager _session;
	private boolean _haveSchedule = false;
	private Element _definition;
	private LocalDateTime _nextRun = LocalDateTime.MAX;
	private LocalDateTime _currentDateTime = LocalDateTime.now();

	private LocalDateTime _start;
	private LocalDateTime _expire;
	private int _interval = -1;
	private int[] _daysOfWeek = new int[] {};
	private int[] _months = new int[] {};
	private int[] _daysOfMonth = new int[] {};

	private String _reason = "Schedule does not apply, incorrectly configured, or is expired.";

	public CalculateSchedule(SessionManager session, Element definition) {
		_session = session;
		_definition = definition;
	}

	public LocalDateTime nextScheduledRun() {
		if (_definition == null) {
			return null;
		} else if ("Schedule".equals(_definition.getNodeName())) {
			parseScheduleElement();
		} else {
			parseScheduleElements();
		}
		if (_haveSchedule) {
			return _nextRun;
		}

		return null;
	}

	public String getReason() {
		return _reason;
	}

	private void parseScheduleElement() {
		scheduleType(_definition);
	}

	private void parseScheduleElements() {
		if (_definition == null) {
			return;
		}

		Object nodeList = XmlUtilities.selectNodes(_definition, ".//Schedule");
		if (nodeList == null) {
			return;
		}
		NodeList nodes = (NodeList) nodeList;
		int length = nodes.getLength();
		for (int i = 0; i < length; i++) {
			Element schedule = (Element) nodes.item(i);
			scheduleType(schedule);
		}
	}

	private void scheduleType(Element schedule) {
		parseScheduleAttributes(schedule);
		if ((_start == LocalDateTime.MIN) || _expire.isBefore(_currentDateTime)) {
			// if no start date time, skip this schedule element.
			_reason = "Skipped because schedule is expired.";
			return;
		} else if (_currentDateTime.isBefore(_start) && _start.isBefore(_nextRun)) {
			_nextRun = _start;
			_haveSchedule = true;
			return;
		}
		switch (schedule.getAttribute("Type").toLowerCase()) {
		case "hourly":
			hourlySchedule(schedule);
			break;
		case "daily":
			dailySchedule(schedule);
			break;
		case "weekly":
			weeklySchedule(schedule);
			break;
		case "monthly":
			monthlySchedule(schedule);
			break;
		default:
			throw new PieException("Missing required Type attribute.");
		}
	}

	private void parseScheduleAttributes(Element schedule) {
		_start = StringUtilities.toDateTime(_session.requiredAttribute(schedule, "Start"), LocalDateTime.MIN);
		_expire = StringUtilities.toDateTime(_session.optionalAttribute(schedule, "Expire"), LocalDateTime.MAX);
		_interval = StringUtilities.toInteger(_session.optionalAttribute(schedule, "Interval"), -1);
		_months = toMonthArray(_session.optionalAttribute(schedule, "Months"));
		_daysOfWeek = toDayOfWeekArray(_session.optionalAttribute(schedule, "DaysOfWeek"));
		_daysOfMonth = toIntegerArray(_session.optionalAttribute(schedule, "DaysOfMonth"));

		validateInterval();

		// Sort the integer arrays (low-high)
		Arrays.sort(_months);
		Arrays.sort(_daysOfMonth);
		Arrays.sort(_daysOfWeek);
	}

	private void hourlySchedule(Element hourly) {
		if (_interval < 1) {
			_reason = "Skipped because interval less than 1";
			return;
		}

		LocalDateTime nextHourRun = LocalDateTime.of(_currentDateTime.getYear(), _currentDateTime.getMonthValue(), _currentDateTime.getDayOfMonth(), _currentDateTime.getHour(), _start.getMinute(), _start.getSecond()).plusHours(_interval);
		while (nextHourRun.isBefore(_currentDateTime)) {
			nextHourRun = nextHourRun.plusHours(_interval);
		}

		if (nextHourRun.isBefore(_nextRun) && nextHourRun.isBefore(_expire)) {
			_nextRun = nextHourRun;
			_haveSchedule = true;
		}
	}

	private void dailySchedule(Element daily) {
		if (_interval < 1) {
			_reason = "Skipped because interval less than 1";
			return;
		}

		LocalDateTime nextDayRun = LocalDateTime.of(_currentDateTime.getYear(), _currentDateTime.getMonthValue(), _currentDateTime.getDayOfMonth(), _start.getHour(), _start.getMinute(), _start.getSecond()).plusDays(_interval);
		while (nextDayRun.isBefore(_currentDateTime)) {
			nextDayRun = nextDayRun.plusDays(_interval);
		}

		if (nextDayRun.isBefore(_nextRun) && nextDayRun.isBefore(_expire)) {
			_nextRun = nextDayRun;
			_haveSchedule = true;
		}
	}

	private void weeklySchedule(Element weekly) {
		// If user has provided both an interval and specific days, specific
		// days takes precedence.
		if (_daysOfWeek.length > 0) {
			// ISO8601: Monday=1 and Sunday=7
			// C# Sunday=0, Saturday=6
			// Java uses enums
			// {S,M,T,W,T,F,S}
			// {0,1,2,3,4,5,6} == C#
			// {7,1,2,3,4,5,6} == ISO (converted to C# values when parsed)
			int currentDayOfWeek = (int) _currentDateTime.getDayOfWeek().getValue();
			int nextDayOfWeek = -1;
			for (int i = 0; i < _daysOfWeek.length; i++) {
				if (_daysOfWeek[i] > currentDayOfWeek) {
					nextDayOfWeek = _daysOfWeek[i];
					break;
				}
			}
			if ((nextDayOfWeek == -1) && (_daysOfWeek.length > 0)) {
				nextDayOfWeek = _daysOfWeek[0];
			} else if ((nextDayOfWeek == -1) && (_daysOfWeek.length == 0)) {
				nextDayOfWeek = (int) _currentDateTime.plusDays(1).getDayOfWeek().getValue();
			}

			LocalDateTime nextDate = LocalDateTime.of(_currentDateTime.getYear(), _currentDateTime.getMonthValue(), _currentDateTime.getDayOfMonth(), _start.getHour(), _start.getMinute(), _start.getSecond());

			for (int i = 1; i < 10; i++) {
				LocalDateTime nextDayOfWeekRun = nextDate.plusDays(i);
				if ((int) nextDayOfWeekRun.getDayOfWeek().getValue() == nextDayOfWeek) {
					_nextRun = nextDayOfWeekRun;
					break;
				}
			}
			_haveSchedule = _nextRun.isBefore(_expire) ? true : false;
			return;
		} else if (_interval > 0) {
			int daysSinceStart = (int) java.time.temporal.ChronoUnit.DAYS.between(_start, _currentDateTime);
			int weeksSinceStart = daysSinceStart / 7;
			LocalDateTime weekStart = _start.plusDays(7 * weeksSinceStart);
			LocalDateTime nextWeekRun = weekStart.plusDays(_interval * 7);
			if (nextWeekRun.isBefore(_nextRun) && nextWeekRun.isBefore(_expire)) {
				_nextRun = nextWeekRun;
				_haveSchedule = true;
			}
		}
	}

	private void monthlySchedule(Element monthly) {
		if ((_months.length == 0) && (_daysOfMonth.length == 0) && (_interval == -1)) {
			return;
		}

		int lastDayOfMonth = _currentDateTime.with(lastDayOfMonth()).getDayOfMonth();
		int nextDayOfMonth = _start.getDayOfMonth();
		LocalDateTime nextDate;

		// Check for any more scheduled days in this month
		if (_daysOfMonth.length > 0) {
			int currentDayOfMonth = _currentDateTime.getDayOfMonth();

			for (int i = 0; i < _daysOfMonth.length; i++) {
				nextDayOfMonth = _daysOfMonth[i];
				if ((nextDayOfMonth > currentDayOfMonth) && (currentDayOfMonth != lastDayOfMonth)) {
					// Adjust for months without that number
					nextDayOfMonth = Math.min(nextDayOfMonth, lastDayOfMonth);
					nextDate = LocalDateTime.of(_currentDateTime.getYear(), _currentDateTime.getMonthValue(), nextDayOfMonth, _start.getHour(), _start.getMinute(), _start.getSecond());
					if (nextDate.isBefore(_nextRun) && nextDate.isBefore(_expire)) {
						_nextRun = nextDate;
						_haveSchedule = true;
						return;
					}
				}
			}

			nextDate = LocalDateTime.of(_currentDateTime.getYear(), _currentDateTime.getMonthValue(), 1, _start.getHour(), _start.getMinute(), _start.getSecond());
			nextDate = nextDate.plusMonths(1);
			lastDayOfMonth = nextDate.with(lastDayOfMonth()).getDayOfMonth();
			nextDayOfMonth = Math.min(_daysOfMonth[0], lastDayOfMonth);
			nextDate = LocalDateTime.of(nextDate.getYear(), nextDate.getMonthValue(), nextDayOfMonth, nextDate.getHour(), nextDate.getMinute(), nextDate.getSecond());
			if (nextDate.isBefore(_nextRun) && nextDate.isBefore(_expire)) {
				_nextRun = nextDate;
				_haveSchedule = true;
				return;
			}
		}

		nextDayOfMonth = (_daysOfMonth.length > 0) ? Math.min(_daysOfMonth[0], lastDayOfMonth) : _start.getDayOfMonth();
		if (_months.length > 0) {
			for (int i = 0; i < _months.length; i++) {
				if ((_months[i] > _currentDateTime.getMonthValue()) && (_months[i] >= 1) && (_months[i] <= 12)) {
					lastDayOfMonth = LocalDateTime.of(_currentDateTime.getYear(), _months[i], 1, 0, 0, 0).with(lastDayOfMonth()).getDayOfMonth();
					nextDayOfMonth = (_daysOfMonth.length > 0) ? Math.min(_daysOfMonth[0], lastDayOfMonth) : _start.getDayOfMonth();
					nextDate = LocalDateTime.of(_currentDateTime.getYear(), _months[i], nextDayOfMonth, _start.getHour(), _start.getMinute(), _start.getSecond());
					if (nextDate.isBefore(_nextRun) && nextDate.isBefore(_expire)) {
						_nextRun = nextDate;
						_haveSchedule = true;
						return;
					}
				}
			}
		}

		if ((_months.length > 0) && (_months[0] <= _currentDateTime.getMonthValue())) {
			int nextYear = _currentDateTime.getYear();
			nextYear++;
			lastDayOfMonth = LocalDateTime.of(nextYear, _months[0], 1, 0, 0, 0).with(lastDayOfMonth()).getDayOfMonth();
			nextDayOfMonth = (_daysOfMonth.length > 0) ? Math.min(_daysOfMonth[0], lastDayOfMonth) : _start.getDayOfMonth();
			nextDate = LocalDateTime.of(nextYear, _months[0], nextDayOfMonth, _start.getHour(), _start.getMinute(), _start.getSecond());
			if (nextDate.isBefore(_nextRun) && nextDate.isBefore(_expire)) {
				_nextRun = nextDate;
				_haveSchedule = true;
				return;
			}
		}

		if (_interval > 0) {
			nextDate = LocalDateTime.of(_currentDateTime.getYear(), _currentDateTime.getMonth(), _start.getDayOfMonth(), _start.getHour(), _start.getMinute(), _start.getSecond());
			nextDate = nextDate.plusMonths(_interval);
			if (nextDate.isBefore(_nextRun) && nextDate.isBefore(_expire)) {
				_nextRun = nextDate;
				_haveSchedule = true;
				return;
			}
		}
	}

	private int[] toIntegerArray(String s) {
		if (StringUtilities.isNullOrEmpty(s)) {
			return new int[] {};
		}

		String[] values = s.split(",");
		List<Integer> intArray = new ArrayList<Integer>();
		for (int i = 0; i < values.length; i++) {
			int value = StringUtilities.toInteger(values[i], -1);
			if (value != -1)
				intArray.add(value);
		}

		return ArrayUtilities.toIntegerArray(intArray);
	}

	private int[] toDayOfWeekArray(String s) {
		if (StringUtilities.isNullOrEmpty(s)) {
			return new int[] {};
		}

		String[] values = s.split(",");
		List<Integer> daysOfWeek = new ArrayList<Integer>();
		for (int i = 0; i < values.length; i++) {
			int value = -1;
			switch (values[i].trim().toLowerCase()) {
			case "mon":
			case "monday":
				value = 1;
				break;
			case "tue":
			case "tuesday":
				value = 2;
				break;
			case "wed":
			case "wednesday":
				value = 3;
				break;
			case "thu":
			case "thursday":
				value = 4;
				break;
			case "fri":
			case "friday":
				value = 5;
				break;
			case "sat":
			case "saturday":
				value = 6;
				break;
			case "sun":
			case "sunday":
				value = 0;
				break;
			default:
				// Convert ISO Day of Week {Sunday=7,1,2,3,4,5,6} to C#
				// {Sunday=0,1,2,3,4,5,6}
				value = StringUtilities.toInteger(values[i], -1);
				break;
			}

			if ((value >= 0) && (value <= 6)) {
				daysOfWeek.add(value);
			}
		}

		return ArrayUtilities.toIntegerArray(daysOfWeek);
	}

	private int[] toMonthArray(String s) {
		if (StringUtilities.isNullOrEmpty(s)) {
			return new int[] {};
		}

		String[] values = s.split(",");
		List<Integer> monthsOfYear = new ArrayList<Integer>();
		for (int i = 0; i < values.length; i++) {
			int value = -1;
			switch (values[i].trim().toLowerCase()) {
			case "jan":
			case "january":
				value = 1;
				break;
			case "feb":
			case "february":
				value = 2;
				break;
			case "mar":
			case "march":
				value = 3;
				break;
			case "apr":
			case "april":
				value = 4;
				break;
			case "may":
				value = 5;
				break;
			case "jun":
			case "june":
				value = 6;
				break;
			case "jul":
			case "july":
				value = 7;
				break;
			case "aug":
			case "august":
				value = 8;
				break;
			case "sep":
			case "september":
				value = 9;
				break;
			case "oct":
			case "october":
				value = 10;
				break;
			case "nov":
			case "november":
				value = 11;
				break;
			case "dec":
			case "december":
				value = 12;
				break;
			default:
				value = StringUtilities.toInteger(values[i], -1);
				break;
			}

			if ((value >= 1) && (value <= 12)) {
				monthsOfYear.add(value);
			}
		}
		return ArrayUtilities.toIntegerArray(monthsOfYear);
	}

	private void validateInterval() {
		// Only accept values positive integers
		if (_interval < 1) {
			_interval = -1;
		}
	}
}
