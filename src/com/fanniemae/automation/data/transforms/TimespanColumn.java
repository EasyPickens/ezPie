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
public class TimespanColumn extends DataTransform {

	protected TransformDateValue _FormatDateValue;

	protected int _DayOfWeekShift = 0;
	protected Calendar _FiscalStart = Calendar.getInstance();

	// Notes: (after testing.)
	// Arrays added to improve performance. Data processing time dropped from
	// 23.5 seconds down to 7.7 seconds on 1 million rows x 30 Date operations
	// on test machine.
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

	public TimespanColumn(SessionManager session, Element operation) {
		this(session, operation, null);
	}

	public TimespanColumn(SessionManager session, Element operation, Calendar fiscalYearStart) {
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
		dataRow[_OutColumnIndex] = _FormatDateValue.transform((Date) dataRow[_SourceColumnIndex]);
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
		// In JAVA returns 0 for January
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

	protected TransformDateValue instantiateFormatMethod(String sTimePeriod) {
		_ColumnType = "java.util.Date";
		switch (sTimePeriod.toLowerCase()) {
		case "date":
			return new DateOnly();
		case "day":
			return new DateOnly();
		case "dayofmonth":
			_ColumnType = "java.lang.Integer";
			return new DayOfMonth();
		case "dayofweek":
			_ColumnType = "java.lang.Integer";
			return new DayOfWeek(_DayOfWeekShift);
		case "dayofweekabbreviation":
			_ColumnType = "java.lang.String";
			return new DayOfWeekAbbreviation(_DayAbbreviations);
		case "dayofweekname":
			_ColumnType = "java.lang.String";
			return new DayOfWeekName(_DayNames);
		case "dayofyear":
			_ColumnType = "java.lang.Integer";
			return new DayOfYear();
		case "firstdayoffiscalquarter":
			initializeFiscalYearArrays();
			return new FiscalQuarterFirstDay(_FiscalQuarters, _FiscalQuarterFirstMonth);
		case "firstdayofmonth":
			return new FirstDayOfMonth();
		case "firstdayofquarter":
			return new FirstDayOfQuarter();
		case "firstdayofweek":
			return new FirstDayOfWeek(_DayOfWeekShift);
		case "firstdayofyear":
			return new FirstDayOfYear();
		case "firsthourofday":
			return new FirstHourOfDay();
		case "firstminuteofhour":
			return new FirstMinuteOfHour();
		case "firstsecondofminute":
			return new FirstSecondOfMinute();
		case "fiscalquarter":
			_ColumnType = "java.lang.Integer";
			initializeFiscalYearArrays();
			return new FiscalQuarter(_FiscalQuarters);
		case "hour":
			_ColumnType = "java.lang.Integer";
			return new JustHour();
		case "lastdayoffiscalquarter":
			initializeFiscalYearArrays();
			return new FiscalQuarterLastDay(_FiscalQuarters, _FiscalQuarterLastMonth);
		case "lastdayofmonth":
			return new LastDayOfMonth();
		case "lastdayofquarter":
			return new LastDayOfQuarter(_Quarters, _QuarterLastMonth);
		case "lastdayofweek":
			return new LastDayOfWeek(_DayOfWeekShift);
		case "lastdayofyear":
			return new LastDayOfYear();
		case "minute":
			_ColumnType = "java.lang.Integer";
			return new JustMinute();
		case "month":
			_ColumnType = "java.lang.Integer";
			return new MonthNumber();
		case "monthabbreviation":
			_ColumnType = "java.lang.String";
			return new MonthAbbrevation(_MonthAbbreviations);
		case "monthname":
			_ColumnType = "java.lang.String";
			return new MonthName(_MonthNames);
		case "quarter":
			_ColumnType = "java.lang.Integer";
			return new QuarterNumber(_Quarters);
		case "second":
			_ColumnType = "java.lang.Integer";
			return new JustSecond();
		case "week":
			_ColumnType = "java.lang.Integer";
			return new WeekOfYear();
		case "year":
			_ColumnType = "java.lang.Integer";
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
			_Calendar.set(Calendar.MONTH, _QuarterFirstMonth[_Quarters[_Calendar.get(Calendar.MONTH)] - 1] - 1);
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
