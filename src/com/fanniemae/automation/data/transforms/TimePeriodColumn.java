package com.fanniemae.automation.data.transforms;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.w3c.dom.Element;

import com.fanniemae.automation.SessionManager;
import com.fanniemae.automation.common.StringUtilities;

/**
 * 
 * @author Richard Monson
 * @since 2016-01-07
 * 
 */
public class TimePeriodColumn extends DataTransform {

	protected FormatDateValue _FormatDateValue;

	protected int _DayOfWeekShift = 0;
	protected Calendar _FiscalStart = Calendar.getInstance();

	// Notes: (after testing.)
	// Arrays added to improve performance. Data processing time dropped from
	// 23.5 seconds down to 7.7 seconds on 1 million rows x 26 Date operations.
	protected String[] _DayAbbreviations = new String[] { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
	protected String[] _DayNames = new String[] { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };
	protected String[] _MonthAbbreviations = new String[] { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
	protected String[] _MonthNames = new String[] { "January", "February", "Marche", "April", "May", "June", "July", "August", "September", "October", "November", "December" };
	protected int[] _Quarters = new int[] { 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4 };
	protected int[] _QuarterFirstMonth = new int[] { 1, 4, 7, 10 };

	protected int[] _FiscalQuarters = new int[] { 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4 };
	protected int[] _FiscalQuarterFirstMonth = new int[] { 1, 4, 7, 10 };

	// Special case: just subtract 1 day to get last day of the previous month.
	protected int[] _QuarterLastMonth = new int[] { 4, 7, 10, 1 };
	protected int[] _FiscalQuarterLastMonth = new int[] { 4, 7, 10, 1 };

	public TimePeriodColumn(SessionManager session, Element operation) {
		this(session, operation, null);
	}

	public TimePeriodColumn(SessionManager session, Element operation, Calendar fiscalYearStart) {
		super(session, operation, false);
		if (fiscalYearStart == null) {
			_FiscalStart.set(Calendar.MONTH, 0);
			_FiscalStart.set(Calendar.DATE, 1);
		} else {
			_FiscalStart = fiscalYearStart;
		}

		_DataColumn = _Session.getAttribute(_Transform, "DataColumn");
		String sTimePeriod = _Session.getAttribute(_Transform, "TimePeriod");
		String sCulture = _Session.getAttribute(_Transform, "TimePeriodCulture");

		if (StringUtilities.isNullOrEmpty(_DataColumn)) {
			throw new RuntimeException("Missing a value in DataColumn for the TimePeriodColumn.");
		}
		if (StringUtilities.isNullOrEmpty(sTimePeriod)) {
			throw new RuntimeException("Missing a value in TimePeroid for the TimePeriodColumn.");
		}

		// Populate the arrays with the correct day and month names.
		populateNameArrays(sCulture);

		_FormatDateValue = instantiateFormatMethod(sTimePeriod);
	}

	@Override
	public boolean isTableLevel() {
		return false;
	}

	@Override
	public Object[] processDataRow(Object[] dataRow) {
		if (dataRow == null) {
			return dataRow;
		}

		dataRow = addDataColumn(dataRow);

		dataRow[_OutColumnIndex] = _FormatDateValue.formatDate((Date) dataRow[_SourceColumnIndex]);
		_RowsProcessed++;
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

		DateFormatSymbols dfs = DateFormatSymbols.getInstance(oNameLocale);
		String[] aDayAbbreviations = dfs.getShortWeekdays();
		String[] aDayNames = dfs.getWeekdays();
		String[] aMonthAbbreviations = dfs.getShortMonths();
		String[] aMonthNames = dfs.getMonths();

		for (int i = 0; i < 7; i++) {
			_DayAbbreviations[i] = aDayAbbreviations[i + 1];
			_DayNames[i] = aDayNames[i + 1];
		}

		for (int i = 0; i < 12; i++) {
			_MonthAbbreviations[i] = aMonthAbbreviations[i + 1];
			_MonthNames[i] = aMonthNames[i + 1];
		}
	}

	protected void initializeFiscalYearArrays() {
		// In JAVA returns 0 for Jan
		if (_FiscalStart.get(Calendar.MONTH) != 0) {
			int iStartMonth = _FiscalStart.get(Calendar.MONTH) + 1;
			// Shift fiscal arrays based on fiscal year start
			int nQ = 1;
			int nMonth = 1;
			int nIndex = iStartMonth - 1;
			_FiscalQuarterFirstMonth[0] = iStartMonth;
			_FiscalQuarterLastMonth[0] = iStartMonth + 3;
			if (_FiscalQuarterLastMonth[0] > 12) {
				_FiscalQuarterLastMonth[0] = _FiscalQuarterFirstMonth[0] - 12;
			}
			for (int i = 0; i < 12; i++) {
				_FiscalQuarters[nIndex] = nQ;
				nMonth++;
				if ((nMonth > 3) && (i < 11)) {
					_FiscalQuarterFirstMonth[nQ] = nIndex + 2;
					_FiscalQuarterLastMonth[nQ] = nIndex + 5;
					if (_FiscalQuarterLastMonth[nQ] > 12) {
						_FiscalQuarterLastMonth[nQ] = _FiscalQuarterLastMonth[nQ] - 12;
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

	protected FormatDateValue instantiateFormatMethod(String sTimePeriod) {
		_ColumnType = "java.util.Date";
		switch (sTimePeriod.toLowerCase()) {
		case "date":
			return new GetDateOnly();
		case "day":
			return new GetDateOnly();
		case "dayofmonth":
			_ColumnType = "java.lang.Integer";
			return new GetDayOfMonth();
		case "dayofweek":
			_ColumnType = "java.lang.Integer";
			return new GetDayOfWeek(_DayOfWeekShift);
		case "dayofweekabbreviation":
			_ColumnType = "java.lang.String";
			return new GetDayOfWeekAbbreviation(_DayAbbreviations);
		case "dayofweekname":
			_ColumnType = "java.lang.String";
			return new GetDayOfWeekName(_DayNames);
		case "dayofyear":
			_ColumnType = "java.lang.Integer";
			return new GetDayOfYear();
		case "firstdayoffiscalquarter":
			initializeFiscalYearArrays();
			return new GetFiscalQuarterFirstDay(_FiscalQuarters, _FiscalQuarterFirstMonth);
		case "firstdayofmonth":
			return new GetFirstDayOfMonth();
		case "firstdayofquarter":
			return new GetFirstDayOfQuarter();
		case "firstdayofweek":
			return new GetFirstDayOfWeek(_DayOfWeekShift);
		case "firstdayofyear":
			return new GetFirstDayOfYear();
		case "firsthourofday":
			return new GetFirstHourOfDay();
		case "firstminuteofhour":
			return new GetFirstMinuteOfHour();
		case "firstsecondofminute":
			return new GetFirstSecondOfMinute();
		case "fiscalquarter":
			_ColumnType = "java.lang.Integer";
			initializeFiscalYearArrays();
			return new GetFiscalQuarter(_FiscalQuarters);
		case "hour":
			_ColumnType = "java.lang.Integer";
			return new GetHour();
		case "lastdayoffiscalquarter":
			initializeFiscalYearArrays();
			return new GetFiscalQuarterLastDay(_FiscalQuarters, _FiscalQuarterLastMonth);
		case "lastdayofmonth":
			return new GetLastDayOfMonth();
		case "lastdayofquarter":
			return new GetLastDayOfQuarter(_Quarters, _QuarterLastMonth);
		case "lastdayofweek":
			return new GetLastDayOfWeek(_DayOfWeekShift);
		case "lastdayofyear":
			return new GetLastDayOfYear();
		case "minute":
			_ColumnType = "java.lang.Integer";
			return new GetMinute();
		case "month":
			_ColumnType = "java.lang.Integer";
			return new GetMonthNumber();
		case "monthabbreviation":
			_ColumnType = "java.lang.String";
			return new GetMonthAbbrevation(_MonthAbbreviations);
		case "monthname":
			_ColumnType = "java.lang.String";
			return new GetMonthName(_MonthNames);
		case "quarter":
			_ColumnType = "java.lang.Integer";
			return new GetQuarter(_Quarters);
		case "second":
			_ColumnType = "java.lang.Integer";
			return new GetSecond();
		case "week":
			_ColumnType = "java.lang.Integer";
			return new GetWeek();
		case "year":
			_ColumnType = "java.lang.Integer";
			return new GetYear();
		default:
			throw new RuntimeException("Invalid TimePeriod attribute for a TimePeriodColumn: " + sTimePeriod);
		}
	}

	protected abstract class FormatDateValue {
		protected Calendar _Calendar = Calendar.getInstance();

		public abstract Object formatDate(Date dateValue);
	}

	protected class GetDateOnly extends FormatDateValue {

		@Override
		public Object formatDate(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.HOUR_OF_DAY, 0);
			_Calendar.set(Calendar.MINUTE, 0);
			_Calendar.set(Calendar.SECOND, 0);
			_Calendar.set(Calendar.MILLISECOND, 0);
			return _Calendar.getTime();
		}
	}

	protected class GetDayOfMonth extends FormatDateValue {

		@Override
		public Object formatDate(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _Calendar.get(Calendar.DATE);
		}
	}

	protected class GetFirstDayOfQuarter extends FormatDateValue {

		@Override
		public Object formatDate(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.MONTH, _QuarterFirstMonth[_Quarters[_Calendar.get(Calendar.MONTH)] - 1]);
			_Calendar.set(Calendar.DATE, 1);
			_Calendar.clear(Calendar.HOUR_OF_DAY);
			_Calendar.clear(Calendar.AM_PM);
			_Calendar.clear(Calendar.MINUTE);
			_Calendar.clear(Calendar.SECOND);
			_Calendar.clear(Calendar.MILLISECOND);
			return _Calendar.getTime();
		}
	}

	protected class GetDayOfWeek extends FormatDateValue {

		protected int _nDayOfWeekShift = 0;

		public GetDayOfWeek(int DayOfWeekShift) {
			super();
			_nDayOfWeekShift = DayOfWeekShift;
		}

		@Override
		public Object formatDate(Date dateValue) {
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

	protected class GetDayOfWeekAbbreviation extends FormatDateValue {

		protected String[] _aDayAbbreviations = new String[7];

		public GetDayOfWeekAbbreviation(String[] aAbbreviations) {
			super();
			_aDayAbbreviations = aAbbreviations;
		}

		@Override
		public Object formatDate(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _aDayAbbreviations[_Calendar.get(Calendar.DAY_OF_WEEK)];
		}
	}

	protected class GetDayOfWeekName extends FormatDateValue {

		protected String[] _aDayNames = new String[7];

		public GetDayOfWeekName(String[] aDayNames) {
			_aDayNames = aDayNames;
		}

		@Override
		public Object formatDate(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _aDayNames[_Calendar.get(Calendar.DAY_OF_WEEK)];
		}
	}

	protected class GetDayOfYear extends FormatDateValue {

		@Override
		public Object formatDate(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _Calendar.get(Calendar.DAY_OF_YEAR);
		}
	}

	protected class GetFirstDayOfMonth extends FormatDateValue {

		@Override
		public Object formatDate(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.DAY_OF_MONTH, 1);
			_Calendar.set(Calendar.HOUR_OF_DAY, 0);
			_Calendar.set(Calendar.MINUTE, 0);
			_Calendar.set(Calendar.SECOND, 0);
			_Calendar.set(Calendar.MILLISECOND, 0);
			return _Calendar.getTime();
		}
	}

	protected class GetFirstDayOfWeek extends FormatDateValue {

		protected int _nDayOfWeekShift = 0;

		public GetFirstDayOfWeek(int DayOfWeekShift) {
			super();
			_nDayOfWeekShift = DayOfWeekShift;
		}

		@Override
		public Object formatDate(Date dateValue) {
			// Reading the Globalization setting for FirstDayOfWeek.
			_Calendar.setTime(dateValue);
			int nDayOfWeek = _Calendar.get(Calendar.DAY_OF_WEEK) - _nDayOfWeekShift;
			if (nDayOfWeek < 0) {
				nDayOfWeek = nDayOfWeek + 7;
			}

			_Calendar.set(Calendar.HOUR_OF_DAY, 0);
			_Calendar.set(Calendar.MINUTE, 0);
			_Calendar.set(Calendar.SECOND, 0);
			_Calendar.set(Calendar.MILLISECOND, 0);
			_Calendar.set(Calendar.DAY_OF_WEEK, nDayOfWeek);
			return _Calendar.getTime();
		}
	}

	protected class GetFirstDayOfYear extends FormatDateValue {

		@Override
		public Object formatDate(Date dateValue) {
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

	protected class GetFirstHourOfDay extends FormatDateValue {

		@Override
		public Object formatDate(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.HOUR_OF_DAY, 0);
			_Calendar.set(Calendar.MINUTE, 0);
			_Calendar.set(Calendar.SECOND, 0);
			_Calendar.set(Calendar.MILLISECOND, 0);
			return _Calendar.getTime();
		}

	}

	protected class GetFirstMinuteOfHour extends FormatDateValue {

		@Override
		public Object formatDate(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.MINUTE, 0);
			_Calendar.set(Calendar.SECOND, 0);
			_Calendar.set(Calendar.MILLISECOND, 0);
			return _Calendar.getTime();
		}
	}

	protected class GetFiscalQuarter extends FormatDateValue {

		protected int[] _aFiscalQuarters = new int[12];

		public GetFiscalQuarter(int[] aFiscalQuarters) {
			super();
			_aFiscalQuarters = aFiscalQuarters;
		}

		@Override
		public Object formatDate(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _aFiscalQuarters[_Calendar.get(Calendar.MONTH)];
		}
	}

	protected class GetFiscalQuarterFirstDay extends FormatDateValue {

		protected int[] _aFiscalQuarters;
		protected int[] _aFiscalQuarterFirstMonth;

		public GetFiscalQuarterFirstDay(int[] aFiscalQuarters, int[] aFiscalQuarterFirstMonth) {
			super();
			_aFiscalQuarters = aFiscalQuarters;
			_aFiscalQuarterFirstMonth = aFiscalQuarterFirstMonth;
		}

		@Override
		public Object formatDate(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.MONTH, _aFiscalQuarterFirstMonth[_aFiscalQuarters[_Calendar.get(Calendar.MONTH)] - 1]);
			_Calendar.set(Calendar.DATE, 1);
			_Calendar.clear(Calendar.HOUR_OF_DAY);
			_Calendar.clear(Calendar.AM_PM);
			_Calendar.clear(Calendar.MINUTE);
			_Calendar.clear(Calendar.SECOND);
			_Calendar.clear(Calendar.MILLISECOND);
			return _Calendar.getTime();
		}
	}

	protected class GetFiscalQuarterLastDay extends FormatDateValue {

		protected int[] _aFiscalQuarters;
		protected int[] _aFiscalQuarterLastMonth;

		public GetFiscalQuarterLastDay(int[] aFiscalQuarters, int[] aFiscalQuarterLastMonth) {
			super();
			_aFiscalQuarters = aFiscalQuarters;
			_aFiscalQuarterLastMonth = aFiscalQuarterLastMonth;
		}

		@Override
		public Object formatDate(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.MONTH, _aFiscalQuarterLastMonth[_aFiscalQuarters[_Calendar.get(Calendar.MONTH)] - 1]);
			_Calendar.set(Calendar.DAY_OF_MONTH, 1);
			_Calendar.set(Calendar.HOUR_OF_DAY, 0);
			_Calendar.set(Calendar.MINUTE, 0);
			_Calendar.set(Calendar.SECOND, 0);
			_Calendar.set(Calendar.MILLISECOND, 0);
			_Calendar.add(Calendar.DAY_OF_MONTH, -1);
			return _Calendar.getTime();
		}
	}

	protected class GetHour extends FormatDateValue {

		@Override
		public Object formatDate(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _Calendar.get(Calendar.HOUR_OF_DAY);
		}
	}

	protected class GetLastDayOfMonth extends FormatDateValue {

		@Override
		public Object formatDate(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.DAY_OF_MONTH, 1);
			_Calendar.add(Calendar.MONTH, 1);
			_Calendar.add(Calendar.DATE, -1);
			_Calendar.set(Calendar.HOUR_OF_DAY, 0);
			_Calendar.set(Calendar.MINUTE, 0);
			_Calendar.set(Calendar.SECOND, 0);
			_Calendar.set(Calendar.MILLISECOND, 0);
			return _Calendar.getTime();
		}
	}

	protected class GetLastDayOfQuarter extends FormatDateValue {

		protected int[] _aQuarters;
		protected int[] _aQuarterLastMonth;

		public GetLastDayOfQuarter(int[] aQuarters, int[] aQuarterLastMonth) {
			_aQuarters = aQuarters;
			_aQuarterLastMonth = aQuarterLastMonth;
		}

		@Override
		public Object formatDate(Date dateValue) {
			{
				_Calendar.setTime(dateValue);
				_Calendar.set(Calendar.MONTH, _aQuarterLastMonth[_aQuarters[_Calendar.get(Calendar.MONTH)] - 1]);
				_Calendar.add(Calendar.DATE, -1);
				_Calendar.set(Calendar.HOUR_OF_DAY, 0);
				_Calendar.set(Calendar.MINUTE, 0);
				_Calendar.set(Calendar.SECOND, 0);
				_Calendar.set(Calendar.MILLISECOND, 0);
				return _Calendar.getTime();
			}
		}
	}

	protected class GetLastDayOfWeek extends FormatDateValue {

		protected int _iDayOfWeekShift = 0;

		public GetLastDayOfWeek(int iDayOfWeekShift) {
			_iDayOfWeekShift = iDayOfWeekShift;
		}

		@Override
		public Object formatDate(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.add(Calendar.DATE, (6 - _Calendar.get(Calendar.DAY_OF_WEEK)) + _iDayOfWeekShift);
			_Calendar.set(Calendar.HOUR_OF_DAY, 0);
			_Calendar.set(Calendar.MINUTE, 0);
			_Calendar.set(Calendar.SECOND, 0);
			_Calendar.set(Calendar.MILLISECOND, 0);
			return _Calendar.getTime();
		}

	}

	protected class GetLastDayOfYear extends FormatDateValue {

		@Override
		public Object formatDate(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.MONTH, 11);
			_Calendar.set(Calendar.DAY_OF_MONTH, 31);
			_Calendar.set(Calendar.HOUR_OF_DAY, 23);
			_Calendar.set(Calendar.MINUTE, 59);
			_Calendar.set(Calendar.SECOND, 59);
			_Calendar.set(Calendar.MILLISECOND, 999);
			return _Calendar.getTime();
		}
	}

	protected class GetLastHourOfDay extends FormatDateValue {

		@Override
		public Object formatDate(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.HOUR_OF_DAY, 23);
			_Calendar.set(Calendar.MINUTE, 59);
			_Calendar.set(Calendar.SECOND, 59);
			_Calendar.set(Calendar.MILLISECOND, 999);
			return _Calendar.getTime();
		}
	}

	protected class GetMinute extends FormatDateValue {

		@Override
		public Object formatDate(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _Calendar.get(Calendar.MINUTE);
		}
	}

	protected class GetMonthAbbrevation extends FormatDateValue {

		protected String[] _aMonthAbbreviations;

		public GetMonthAbbrevation(String[] aMonthAbbreviations) {
			_aMonthAbbreviations = aMonthAbbreviations;
		}

		@Override
		public Object formatDate(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _aMonthAbbreviations[_Calendar.get(Calendar.MONTH)];
		}
	}

	protected class GetMonthName extends FormatDateValue {

		protected String[] _aMonthNames;

		public GetMonthName(String[] aMonthNames) {
			_aMonthNames = aMonthNames;
		}

		@Override
		public Object formatDate(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _aMonthNames[_Calendar.get(Calendar.MONTH)];
		}
	}

	protected class GetMonthNumber extends FormatDateValue {

		@Override
		public Object formatDate(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _Calendar.get(Calendar.MONTH) + 1;
		}
	}

	protected class GetQuarter extends FormatDateValue {

		protected int[] _aQuarters;

		public GetQuarter(int[] aQuarters) {
			_aQuarters = aQuarters;
		}

		@Override
		public Object formatDate(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _aQuarters[_Calendar.get(Calendar.MONTH)];
		}
	}

	protected class GetSecond extends FormatDateValue {

		@Override
		public Object formatDate(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _Calendar.get(Calendar.SECOND);
		}
	}

	protected class GetFirstSecondOfMinute extends FormatDateValue {

		@Override
		public Object formatDate(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.SECOND, 0);
			return _Calendar.getTime();
		}
	}

	protected class GetWeek extends FormatDateValue {

		@Override
		public Object formatDate(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _Calendar.get(Calendar.WEEK_OF_YEAR);
		}
	}

	protected class GetYear extends FormatDateValue {

		@Override
		public Object formatDate(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _Calendar.get(Calendar.YEAR);
		}
	}

}
