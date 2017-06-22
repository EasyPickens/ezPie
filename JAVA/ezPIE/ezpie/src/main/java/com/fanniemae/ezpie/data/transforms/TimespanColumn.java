/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.data.transforms;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.DateUtilities;
import com.fanniemae.ezpie.common.StringUtilities;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-01-07
 * 
*/

public class TimespanColumn extends DataTransform {

	protected TransformDateValue _formatDateValue;

	protected int _dayOfWeekShift = 0;
	protected Calendar _fiscalStart = Calendar.getInstance();

	/**
	 * Notes:
	 *  
	 * After testing the code, I decided to add populated name arrays in order to improve performance.
	 * My i5-3570K CPU took 25.3 seconds using standard java calls, when I switched to pre-populated name
	 * arrays the time dropped to 7.2 seconds.
	 * 
	 * The data set was my standard 1 million row table and consisted of 30 simultaneous date operation.
	 * on test machine.
	 * 
	 * The value's in the name arrays are update based on the machine locale setting.
	 * 
	 */
	
	protected String[] _dayAbbreviations = new String[] { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
	protected String[] _dayNames = new String[] { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };
	protected String[] _monthAbbreviations = new String[] { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
	protected String[] _monthNames = new String[] { "January", "February", "Marche", "April", "May", "June", "July", "August", "September", "October", "November", "December" };
	protected int[] _quarters = new int[] { 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4 };
	protected int[] _quarterFirstMonth = new int[] { 1, 4, 7, 10 };

	protected int[] _fiscalQuarters = new int[] { 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4 };
	protected int[] _fiscalQuarterFirstMonth = new int[] { 1, 4, 7, 10 };

	// Special case: just subtract 1 day to get last day of the previous month.
	protected int[] _quarterLastMonth = new int[] { 4, 7, 10, 1 };
	protected int[] _fiscalQuarterLastMonth = new int[] { 4, 7, 10, 1 };

	public TimespanColumn(SessionManager session, Element transform) {
		this(session, transform, null);
	}

	public TimespanColumn(SessionManager session, Element transform, Calendar fiscalYearStart) {
		super(session, transform, false);
		if (fiscalYearStart == null) {
			_fiscalStart.set(Calendar.MONTH, 0);
			_fiscalStart.set(Calendar.DATE, 1);
		} else {
			_fiscalStart = fiscalYearStart;
		}

		_dataColumn = _session.getAttribute(_transform, "DataColumn");
		String sTimePeriod = _session.getAttribute(_transform, "TimePeriod");
		String sCulture = _session.getAttribute(_transform, "TimePeriodCulture");

		if (StringUtilities.isNullOrEmpty(_dataColumn)) {
			throw new RuntimeException("Missing a value in DataColumn for the TimePeriodColumn.");
		}
		if (StringUtilities.isNullOrEmpty(sTimePeriod)) {
			throw new RuntimeException("Missing a value in TimePeroid for the TimePeriodColumn.");
		}
		_transformInfo.appendFormatLine("DataColumn = %s", _dataColumn);
		if (StringUtilities.isNotNullOrEmpty(sCulture)) {
			_transformInfo.appendFormatLine("TimePeriodCulture = %s", sCulture);
		}
		if (fiscalYearStart != null) {
			_transformInfo.appendFormatLine("Fiscal Year Start = %s", DateUtilities.toIsoString(fiscalYearStart));
		}
		_transformInfo.appendFormat("TimePeriod = %s", sTimePeriod);
		
		// Populate the arrays with the server culture day and month names.
		populateNameArrays(sCulture);

		_formatDateValue = inializeFormatClass(sTimePeriod);
	}

	@Override
	public boolean isolated() {
		return false;
	}

	@Override
	public Object[] processDataRow(Object[] dataRow) {
		if (dataRow == null) {
			return dataRow;
		}
		dataRow = addDataColumn(dataRow);
		dataRow[_outColumnIndex] = _formatDateValue.transform((Date) dataRow[_sourceColumnIndex]);
		_rowsProcessed++;
		return dataRow;
	}

	protected void populateNameArrays(String sUserCulture) {
		if (StringUtilities.isNullOrEmpty(sUserCulture)) {
			return;
		}

		Locale oNameLocale;
		try {
			oNameLocale = new Locale(sUserCulture);
		} catch (Exception ex) {
			try {
				oNameLocale = new Locale(sUserCulture.substring(0, sUserCulture.indexOf('-')));
			} catch (Exception ee) {
				return;
			}
		}

		// Will populate name arrays with correct name/abbreviation based on machine locale settings
		DateFormatSymbols dfs = DateFormatSymbols.getInstance(oNameLocale);
		String[] aDayAbbreviations = dfs.getShortWeekdays();
		String[] aDayNames = dfs.getWeekdays();
		String[] aMonthAbbreviations = dfs.getShortMonths();
		String[] aMonthNames = dfs.getMonths();

		for (int i = 0; i < 7; i++) {
			_dayAbbreviations[i] = aDayAbbreviations[i + 1];
			_dayNames[i] = aDayNames[i + 1];
		}

		for (int i = 0; i < 12; i++) {
			_monthAbbreviations[i] = aMonthAbbreviations[i + 1];
			_monthNames[i] = aMonthNames[i + 1];
		}
	}

	protected void initializeFiscalYearArrays() {
		// In JAVA returns 0 for January
		if (_fiscalStart.get(Calendar.MONTH) != 0) {
			int iStartMonth = _fiscalStart.get(Calendar.MONTH) + 1;
			// Shift fiscal arrays based on fiscal year start
			int nQ = 1;
			int nMonth = 1;
			int nIndex = iStartMonth - 1;
			_fiscalQuarterFirstMonth[0] = iStartMonth;
			_fiscalQuarterLastMonth[0] = iStartMonth + 3;
			if (_fiscalQuarterLastMonth[0] > 12) {
				_fiscalQuarterLastMonth[0] = _fiscalQuarterFirstMonth[0] - 12;
			}
			for (int i = 0; i < 12; i++) {
				_fiscalQuarters[nIndex] = nQ;
				nMonth++;
				if ((nMonth > 3) && (i < 11)) {
					_fiscalQuarterFirstMonth[nQ] = nIndex + 2;
					_fiscalQuarterLastMonth[nQ] = nIndex + 5;
					if (_fiscalQuarterLastMonth[nQ] > 12) {
						_fiscalQuarterLastMonth[nQ] = _fiscalQuarterLastMonth[nQ] - 12;
					}
					nQ++;
					nMonth = 1;
				}
				nIndex++;
				if (nIndex > 11) {
					nIndex = 0;
				}
			}
		}
	}

	protected TransformDateValue inializeFormatClass(String sTimePeriod) {
		_columnType = "java.util.Date";
		switch (sTimePeriod.toLowerCase()) {
		case "date":
			return new DateOnly();
		case "day":
			return new DateOnly();
		case "dayofmonth":
			_columnType = "java.lang.Integer";
			return new DayOfMonth();
		case "dayofweek":
			_columnType = "java.lang.Integer";
			return new DayOfWeek(_dayOfWeekShift);
		case "dayofweekabbreviation":
			_columnType = "java.lang.String";
			return new DayOfWeekAbbreviation(_dayAbbreviations);
		case "dayofweekname":
			_columnType = "java.lang.String";
			return new DayOfWeekName(_dayNames);
		case "dayofyear":
			_columnType = "java.lang.Integer";
			return new DayOfYear();
		case "firstdayoffiscalquarter":
			initializeFiscalYearArrays();
			return new FiscalQuarterFirstDay(_fiscalQuarters, _fiscalQuarterFirstMonth);
		case "firstdayofmonth":
			return new FirstDayOfMonth();
		case "firstdayofquarter":
			return new FirstDayOfQuarter();
		case "firstdayofweek":
			return new FirstDayOfWeek(_dayOfWeekShift);
		case "firstdayofyear":
			return new FirstDayOfYear();
		case "firsthourofday":
			return new FirstHourOfDay();
		case "firstminuteofhour":
			return new FirstMinuteOfHour();
		case "firstsecondofminute":
			return new FirstSecondOfMinute();
		case "fiscalquarter":
			_columnType = "java.lang.Integer";
			initializeFiscalYearArrays();
			return new FiscalQuarter(_fiscalQuarters);
		case "hour":
			_columnType = "java.lang.Integer";
			return new JustHour();
		case "lastdayoffiscalquarter":
			initializeFiscalYearArrays();
			return new FiscalQuarterLastDay(_fiscalQuarters, _fiscalQuarterLastMonth);
		case "lastdayofmonth":
			return new LastDayOfMonth();
		case "lastdayofquarter":
			return new LastDayOfQuarter(_quarters, _quarterLastMonth);
		case "lastdayofweek":
			return new LastDayOfWeek(_dayOfWeekShift);
		case "lastdayofyear":
			return new LastDayOfYear();
		case "minute":
			_columnType = "java.lang.Integer";
			return new JustMinute();
		case "month":
			_columnType = "java.lang.Integer";
			return new MonthNumber();
		case "monthabbreviation":
			_columnType = "java.lang.String";
			return new MonthAbbrevation(_monthAbbreviations);
		case "monthname":
			_columnType = "java.lang.String";
			return new MonthName(_monthNames);
		case "quarter":
			_columnType = "java.lang.Integer";
			return new QuarterNumber(_quarters);
		case "second":
			_columnType = "java.lang.Integer";
			return new JustSecond();
		case "week":
			_columnType = "java.lang.Integer";
			return new WeekOfYear();
		case "year":
			_columnType = "java.lang.Integer";
			return new JustYear();
		default:
			throw new RuntimeException("Invalid TimePeriod attribute for a TimePeriodColumn: " + sTimePeriod);
		}
	}

	protected abstract class TransformDateValue {
		protected Calendar _Calendar = Calendar.getInstance();

		public abstract Object transform(Date dateValue);
	}

	protected class DateOnly extends TransformDateValue {

		@Override
		public Object transform(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.HOUR_OF_DAY, 0);
			_Calendar.set(Calendar.MINUTE, 0);
			_Calendar.set(Calendar.SECOND, 0);
			_Calendar.set(Calendar.MILLISECOND, 0);
			return _Calendar.getTime();
		}
	}

	protected class DayOfMonth extends TransformDateValue {

		@Override
		public Object transform(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _Calendar.get(Calendar.DATE);
		}
	}

	protected class FirstDayOfQuarter extends TransformDateValue {

		@Override
		public Object transform(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.MONTH, _quarterFirstMonth[_quarters[_Calendar.get(Calendar.MONTH)] - 1] - 1);
			_Calendar.set(Calendar.DATE, 1);
			_Calendar.set(Calendar.HOUR, 0);
			_Calendar.set(Calendar.HOUR_OF_DAY, 0);
			_Calendar.set(Calendar.MINUTE, 0);
			_Calendar.set(Calendar.SECOND, 0);
			_Calendar.set(Calendar.MILLISECOND, 0);
			return _Calendar.getTime();
		}
	}

	protected class DayOfWeek extends TransformDateValue {

		protected int _nDayOfWeekShift = 0;

		public DayOfWeek(int DayOfWeekShift) {
			super();
			_nDayOfWeekShift = DayOfWeekShift;
		}

		@Override
		public Object transform(Date dateValue) {
			// Reading the Globalization setting for
			// FirstDayOfWeek.
			_Calendar.setTime(dateValue);
			int nDayOfWeek = _Calendar.get(Calendar.DAY_OF_WEEK) - _nDayOfWeekShift;
			if (nDayOfWeek < 0) {
				nDayOfWeek = nDayOfWeek + 7;
			}
			return nDayOfWeek;
		}
	}

	protected class DayOfWeekAbbreviation extends TransformDateValue {

		protected String[] _aDayAbbreviations = new String[7];

		public DayOfWeekAbbreviation(String[] aAbbreviations) {
			super();
			_aDayAbbreviations = aAbbreviations;
		}

		@Override
		public Object transform(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _aDayAbbreviations[_Calendar.get(Calendar.DAY_OF_WEEK) - 1];
		}
	}

	protected class DayOfWeekName extends TransformDateValue {

		protected String[] _aDayNames = new String[7];

		public DayOfWeekName(String[] aDayNames) {
			_aDayNames = aDayNames;
		}

		@Override
		public Object transform(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _aDayNames[_Calendar.get(Calendar.DAY_OF_WEEK) - 1];
		}
	}

	protected class DayOfYear extends TransformDateValue {

		@Override
		public Object transform(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _Calendar.get(Calendar.DAY_OF_YEAR);
		}
	}

	protected class FirstDayOfMonth extends TransformDateValue {

		@Override
		public Object transform(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.DAY_OF_MONTH, 1);
			_Calendar.set(Calendar.HOUR_OF_DAY, 0);
			_Calendar.set(Calendar.MINUTE, 0);
			_Calendar.set(Calendar.SECOND, 0);
			_Calendar.set(Calendar.MILLISECOND, 0);
			return _Calendar.getTime();
		}
	}

	protected class FirstDayOfWeek extends TransformDateValue {

		protected int _nDayOfWeekShift = 0;

		public FirstDayOfWeek(int DayOfWeekShift) {
			super();
			_nDayOfWeekShift = DayOfWeekShift;
		}

		@Override
		public Object transform(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.HOUR_OF_DAY, 0);
			_Calendar.set(Calendar.MINUTE, 0);
			_Calendar.set(Calendar.SECOND, 0);
			_Calendar.set(Calendar.MILLISECOND, 0);
			_Calendar.set(Calendar.DAY_OF_WEEK, 1);
			return _Calendar.getTime();
		}
	}

	protected class FirstDayOfYear extends TransformDateValue {

		@Override
		public Object transform(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.MONTH, 0);
			_Calendar.set(Calendar.DAY_OF_MONTH, 1);
			_Calendar.set(Calendar.HOUR_OF_DAY, 0);
			_Calendar.set(Calendar.MINUTE, 0);
			_Calendar.set(Calendar.SECOND, 0);
			_Calendar.set(Calendar.MILLISECOND, 0);
			return _Calendar.getTime();
		}
	}

	protected class FirstHourOfDay extends TransformDateValue {

		@Override
		public Object transform(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.HOUR_OF_DAY, 0);
			_Calendar.set(Calendar.MINUTE, 0);
			_Calendar.set(Calendar.SECOND, 0);
			_Calendar.set(Calendar.MILLISECOND, 0);
			return _Calendar.getTime();
		}

	}

	protected class FirstMinuteOfHour extends TransformDateValue {

		@Override
		public Object transform(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.MINUTE, 0);
			_Calendar.set(Calendar.SECOND, 0);
			_Calendar.set(Calendar.MILLISECOND, 0);
			return _Calendar.getTime();
		}
	}

	protected class FiscalQuarter extends TransformDateValue {

		protected int[] _aFiscalQuarters = new int[12];

		public FiscalQuarter(int[] aFiscalQuarters) {
			super();
			_aFiscalQuarters = aFiscalQuarters;
		}

		@Override
		public Object transform(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _aFiscalQuarters[_Calendar.get(Calendar.MONTH)];
		}
	}

	protected class FiscalQuarterFirstDay extends TransformDateValue {

		protected int[] _aFiscalQuarters;
		protected int[] _aFiscalQuarterFirstMonth;

		protected Calendar _CurrentValue = Calendar.getInstance();

		public FiscalQuarterFirstDay(int[] aFiscalQuarters, int[] aFiscalQuarterFirstMonth) {
			super();
			_aFiscalQuarters = aFiscalQuarters;
			_aFiscalQuarterFirstMonth = aFiscalQuarterFirstMonth;
		}

		@Override
		public Object transform(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.MONTH, _aFiscalQuarterFirstMonth[_aFiscalQuarters[_Calendar.get(Calendar.MONTH)] - 1] - 1);
			_Calendar.set(Calendar.DATE, 1);
			_Calendar.clear(Calendar.HOUR_OF_DAY);
			_Calendar.clear(Calendar.HOUR);
			_Calendar.clear(Calendar.AM_PM);
			_Calendar.clear(Calendar.MINUTE);
			_Calendar.clear(Calendar.SECOND);
			_Calendar.clear(Calendar.MILLISECOND);

			_CurrentValue.setTime(dateValue);

			if (_Calendar.compareTo(_CurrentValue) > 0) {
				_Calendar.add(Calendar.YEAR, -1);
			}

			return _Calendar.getTime();
		}
	}

	protected class FiscalQuarterLastDay extends TransformDateValue {

		protected int[] _aFiscalQuarters;
		protected int[] _aFiscalQuarterLastMonth;

		public FiscalQuarterLastDay(int[] aFiscalQuarters, int[] aFiscalQuarterLastMonth) {
			super();
			_aFiscalQuarters = aFiscalQuarters;
			_aFiscalQuarterLastMonth = aFiscalQuarterLastMonth;
		}

		@Override
		public Object transform(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.MONTH, _aFiscalQuarterLastMonth[_aFiscalQuarters[_Calendar.get(Calendar.MONTH)] - 1] - 1);
			_Calendar.set(Calendar.DAY_OF_MONTH, 1);
			_Calendar.set(Calendar.HOUR_OF_DAY, 0);
			_Calendar.set(Calendar.HOUR, 0);
			_Calendar.set(Calendar.MINUTE, 0);
			_Calendar.set(Calendar.SECOND, 0);
			_Calendar.set(Calendar.MILLISECOND, 0);
			_Calendar.add(Calendar.DAY_OF_MONTH, -1);
			return _Calendar.getTime();
		}
	}

	protected class JustHour extends TransformDateValue {

		@Override
		public Object transform(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _Calendar.get(Calendar.HOUR_OF_DAY);
		}
	}

	protected class LastDayOfMonth extends TransformDateValue {

		@Override
		public Object transform(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.DAY_OF_MONTH, 1);
			_Calendar.add(Calendar.MONTH, 1);
			_Calendar.add(Calendar.DATE, -1);
			_Calendar.set(Calendar.HOUR_OF_DAY, 0);
			_Calendar.set(Calendar.HOUR, 0);
			_Calendar.set(Calendar.MINUTE, 0);
			_Calendar.set(Calendar.SECOND, 0);
			_Calendar.set(Calendar.MILLISECOND, 0);
			return _Calendar.getTime();
		}
	}

	protected class LastDayOfQuarter extends TransformDateValue {

		protected int[] _aQuarters;
		protected int[] _aQuarterLastMonth;

		public LastDayOfQuarter(int[] aQuarters, int[] aQuarterLastMonth) {
			_aQuarters = aQuarters;
			_aQuarterLastMonth = aQuarterLastMonth;
		}

		@Override
		public Object transform(Date dateValue) {
			{
				_Calendar.setTime(dateValue);
				_Calendar.set(Calendar.DAY_OF_MONTH, 1);
				_Calendar.set(Calendar.MONTH, _aQuarterLastMonth[_aQuarters[_Calendar.get(Calendar.MONTH)] - 1] - 1);
				_Calendar.add(Calendar.DATE, -1);
				_Calendar.set(Calendar.HOUR_OF_DAY, 0);
				_Calendar.set(Calendar.HOUR, 0);
				_Calendar.set(Calendar.MINUTE, 0);
				_Calendar.set(Calendar.SECOND, 0);
				_Calendar.set(Calendar.MILLISECOND, 0);
				return _Calendar.getTime();
			}
		}
	}

	protected class LastDayOfWeek extends TransformDateValue {

		protected int _iDayOfWeekShift = 0;

		public LastDayOfWeek(int iDayOfWeekShift) {
			_iDayOfWeekShift = iDayOfWeekShift;
		}

		@Override
		public Object transform(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.HOUR_OF_DAY, 0);
			_Calendar.set(Calendar.MINUTE, 0);
			_Calendar.set(Calendar.SECOND, 0);
			_Calendar.set(Calendar.MILLISECOND, 0);
			_Calendar.set(Calendar.DAY_OF_WEEK, 7);

			return _Calendar.getTime();
		}

	}

	protected class LastDayOfYear extends TransformDateValue {

		@Override
		public Object transform(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.MONTH, 11);
			_Calendar.set(Calendar.DAY_OF_MONTH, 31);
			_Calendar.set(Calendar.HOUR_OF_DAY, 0);
			_Calendar.set(Calendar.MINUTE, 0);
			_Calendar.set(Calendar.SECOND, 0);
			_Calendar.set(Calendar.MILLISECOND, 0);
			return _Calendar.getTime();
		}
	}

	protected class LastHourOfDay extends TransformDateValue {

		@Override
		public Object transform(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.HOUR_OF_DAY, 23);
			_Calendar.set(Calendar.MINUTE, 0);
			_Calendar.set(Calendar.SECOND, 0);
			_Calendar.set(Calendar.MILLISECOND, 0);
			return _Calendar.getTime();
		}
	}

	protected class JustMinute extends TransformDateValue {

		@Override
		public Object transform(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _Calendar.get(Calendar.MINUTE);
		}
	}

	protected class MonthAbbrevation extends TransformDateValue {

		protected String[] _aMonthAbbreviations;

		public MonthAbbrevation(String[] aMonthAbbreviations) {
			_aMonthAbbreviations = aMonthAbbreviations;
		}

		@Override
		public Object transform(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _aMonthAbbreviations[_Calendar.get(Calendar.MONTH)];
		}
	}

	protected class MonthName extends TransformDateValue {

		protected String[] _aMonthNames;

		public MonthName(String[] aMonthNames) {
			_aMonthNames = aMonthNames;
		}

		@Override
		public Object transform(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _aMonthNames[_Calendar.get(Calendar.MONTH)];
		}
	}

	protected class MonthNumber extends TransformDateValue {

		@Override
		public Object transform(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _Calendar.get(Calendar.MONTH) + 1;
		}
	}

	protected class QuarterNumber extends TransformDateValue {

		protected int[] _aQuarters;

		public QuarterNumber(int[] aQuarters) {
			_aQuarters = aQuarters;
		}

		@Override
		public Object transform(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _aQuarters[_Calendar.get(Calendar.MONTH)];
		}
	}

	protected class JustSecond extends TransformDateValue {

		@Override
		public Object transform(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _Calendar.get(Calendar.SECOND);
		}
	}

	protected class FirstSecondOfMinute extends TransformDateValue {

		@Override
		public Object transform(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.SECOND, 0);
			return _Calendar.getTime();
		}
	}

	protected class WeekOfYear extends TransformDateValue {

		@Override
		public Object transform(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _Calendar.get(Calendar.WEEK_OF_YEAR);
		}
	}

	protected class JustYear extends TransformDateValue {

		@Override
		public Object transform(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _Calendar.get(Calendar.YEAR);
		}
	}

}
