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

	protected FormatTimePeriod _FormatTimePeriod;

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
			_FiscalStart.set(Calendar.MONTH, 1);
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
		PopulateNameArrays(sCulture);

		_FormatTimePeriod = DefineProcessingMethod(sTimePeriod);
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

		dataRow = AddDataColumn(dataRow);

		dataRow[_OutColumnIndex] = _FormatTimePeriod.FormatValue((Date)dataRow[_SourceColumnIndex]);
		_RowsProcessed++;
		return dataRow;
	}

	protected void PopulateNameArrays(String sUserCulture) {
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

	protected void InitializeFiscalYearArrays() {
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

	protected FormatTimePeriod DefineProcessingMethod(String sTimePeriod) {
		_ColumnType = "java.util.Date"; //"DateTime";
		switch (sTimePeriod) {
		case "Date":
			return new GetDateOnly();
		case "Day":
			return new GetDateOnly();
		case "DayOfMonth":
			_ColumnType = "java.lang.Integer";
			return new GetDayOfMonth();
		case "DayOfWeek":
			_ColumnType = "java.lang.Integer";
			return new GetDayOfWeek(_DayOfWeekShift);
		case "DayOfWeekAbbreviation":
			_ColumnType = "java.lang.String";
			return new GetDayOfWeekAbbreviation(_DayAbbreviations);
		case "DayOfWeekName":
			_ColumnType = "java.lang.String";
			return new GetDayOfWeekName(_DayNames);
		case "DayOfYear":
			_ColumnType = "java.lang.Integer";
			return new GetDayOfYear();
		case "FirstDayOfFiscalQuarter":
			InitializeFiscalYearArrays();
			return new GetFiscalQuarterFirstDay(_FiscalQuarters, _FiscalQuarterFirstMonth);
		case "FirstDayOfMonth":
			return new GetFirstDayOfMonth();
		case "FirstDayOfQuarter":
			return new GetFirstDayOfQuarter();
		case "FirstDayOfWeek":
			return new GetFirstDayOfWeek(_DayOfWeekShift);
		case "FirstDayOfYear":
			return new GetFirstDayOfYear();
		case "FirstHourOfDay":
			return new GetFirstHourOfDay();
		case "FirstMillisecondOfSecond":
			return new GetFirstMillisecondOfSecond();
		case "FirstMinuteOfHour":
			return new GetFirstMinuteOfHour();
		case "FirstSecondOfMinute":
			return new GetFirstSecondOfMinute();
		case "FiscalQuarter":
			_ColumnType = "java.lang.Integer";
			InitializeFiscalYearArrays();
			return new GetFiscalQuarter(_FiscalQuarters);
		case "Hour":
			_ColumnType = "java.lang.Integer";
			return new GetHour();
		case "LastDayOfFiscalQuarter":
			InitializeFiscalYearArrays();
			return new GetFiscalQuarterLastDay(_FiscalQuarters, _FiscalQuarterLastMonth);
		case "LastDayOfMonth":
			return new GetLastDayOfMonth();
		case "LastDayOfQuarter":
			return new GetLastDayOfQuarter(_Quarters, _QuarterLastMonth);
		case "LastDayOfWeek":
			return new GetLastDayOfWeek(_DayOfWeekShift);
		case "LastDayOfYear":
			return new GetLastDayOfYear();
		case "Minute":
			_ColumnType = "java.lang.Integer";
			return new GetMinute();
		case "Month":
			_ColumnType = "java.lang.Integer";
			return new GetMonthNumber();
		case "MonthAbbreviation":
			_ColumnType = "java.lang.String";
			return new GetMonthAbbrevation(_MonthAbbreviations);
		case "MonthName":
			_ColumnType = "java.lang.String";
			return new GetMonthName(_MonthNames);
		case "Quarter":
			_ColumnType = "java.lang.Integer";
			return new GetQuarter(_Quarters);
		case "Second":
			_ColumnType = "java.lang.Integer";
			return new GetSecond();
		case "Week":
			_ColumnType = "java.lang.Integer";
			return new GetWeek();
		case "Year":
			_ColumnType = "java.lang.Integer";
			return new GetYear();
		default:
			throw new RuntimeException("Invalid TimePeriod attribute for a TimePeriodColumn: " + sTimePeriod);
		}
	}

	protected abstract class FormatTimePeriod {
		protected Calendar _Calendar = Calendar.getInstance();

		public abstract Object FormatValue(Date dateValue);
	}

	protected class GetDateOnly extends FormatTimePeriod {

		@Override
		public Object FormatValue(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.HOUR_OF_DAY, 0);
			_Calendar.set(Calendar.MINUTE, 0);
			_Calendar.set(Calendar.SECOND, 0);
			_Calendar.set(Calendar.MILLISECOND, 0);
			return _Calendar.getTime();
		}
	}

	protected class GetDayOfMonth extends FormatTimePeriod {

		@Override
		public Object FormatValue(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _Calendar.get(Calendar.DATE);
		}
	}

	protected class GetFirstDayOfQuarter extends FormatTimePeriod {

		@Override
		public Object FormatValue(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.MONTH, _QuarterFirstMonth[_Quarters[_Calendar.get(Calendar.MONTH) - 1] - 1]);
			_Calendar.set(Calendar.DATE, 1);
			_Calendar.clear(Calendar.HOUR_OF_DAY);
			_Calendar.clear(Calendar.AM_PM);
			_Calendar.clear(Calendar.MINUTE);
			_Calendar.clear(Calendar.SECOND);
			_Calendar.clear(Calendar.MILLISECOND);
			return _Calendar.getTime();
			// return new Date(dateValue.getYear(),
			// _QuarterFirstMonth[_Quarters[dateValue.getMonth() - 1] - 1], 1);
		}
	}

	protected class GetDayOfWeek extends FormatTimePeriod {

		protected int _nDayOfWeekShift = 0;

		public GetDayOfWeek(int DayOfWeekShift) {
			super();
			_nDayOfWeekShift = DayOfWeekShift;
		}

		@Override
		public Object FormatValue(Date dateValue) {
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

	protected class GetDayOfWeekAbbreviation extends FormatTimePeriod {

		protected String[] _aDayAbbreviations = new String[7];

		public GetDayOfWeekAbbreviation(String[] aAbbreviations) {
			super();
			_aDayAbbreviations = aAbbreviations;
		}

		@Override
		public Object FormatValue(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _aDayAbbreviations[_Calendar.get(Calendar.DAY_OF_WEEK)];
		}
	}

	protected class GetDayOfWeekName extends FormatTimePeriod {

		protected String[] _aDayNames = new String[7];

		public GetDayOfWeekName(String[] aDayNames) {
			_aDayNames = aDayNames;
		}

		@Override
		public Object FormatValue(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _aDayNames[_Calendar.get(Calendar.DAY_OF_WEEK)];
		}
	}

	protected class GetDayOfYear extends FormatTimePeriod {

		@Override
		public Object FormatValue(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _Calendar.get(Calendar.DAY_OF_YEAR);
		}
	}

	protected class GetFirstDayOfMonth extends FormatTimePeriod {

		@Override
		public Object FormatValue(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.DAY_OF_MONTH, 1);
			_Calendar.set(Calendar.HOUR_OF_DAY, 0);
			_Calendar.set(Calendar.MINUTE, 0);
			_Calendar.set(Calendar.SECOND, 0);
			_Calendar.set(Calendar.MILLISECOND, 0);
			return _Calendar.getTime();
		}
	}

	protected class GetFirstDayOfWeek extends FormatTimePeriod {

		protected int _nDayOfWeekShift = 0;

		public GetFirstDayOfWeek(int DayOfWeekShift) {
			super();
			_nDayOfWeekShift = DayOfWeekShift;
		}

		@Override
		public Object FormatValue(Date dateValue) {
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

	protected class GetFirstDayOfYear extends FormatTimePeriod {

		@Override
		public Object FormatValue(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.MONTH, 1);
			_Calendar.set(Calendar.DAY_OF_MONTH, 1);
			_Calendar.set(Calendar.HOUR_OF_DAY, 0);
			_Calendar.set(Calendar.MINUTE, 0);
			_Calendar.set(Calendar.SECOND, 0);
			_Calendar.set(Calendar.MILLISECOND, 0);
			return _Calendar.getTime();
		}
	}

	protected class GetFirstHourOfDay extends FormatTimePeriod {

		@Override
		public Object FormatValue(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.HOUR_OF_DAY, 0);
			_Calendar.set(Calendar.MINUTE, 0);
			_Calendar.set(Calendar.SECOND, 0);
			_Calendar.set(Calendar.MILLISECOND, 0);
			return _Calendar.getTime();
		}

	}

	protected class GetFirstMillisecondOfSecond extends FormatTimePeriod {

		@Override
		public Object FormatValue(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.MILLISECOND, 0);
			return _Calendar.getTime();
		}
	}

	protected class GetFirstMinuteOfHour extends FormatTimePeriod {

		@Override
		public Object FormatValue(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.MINUTE, 0);
			_Calendar.set(Calendar.SECOND, 0);
			_Calendar.set(Calendar.MILLISECOND, 0);
			return _Calendar.getTime();
		}
	}

	protected class GetFiscalQuarter extends FormatTimePeriod {

		protected int[] _aFiscalQuarters = new int[12];

		public GetFiscalQuarter(int[] aFiscalQuarters) {
			super();
			_aFiscalQuarters = aFiscalQuarters;
		}

		@Override
		public Object FormatValue(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _aFiscalQuarters[_Calendar.get(Calendar.MONTH) - 1];
		}
	}

	protected class GetFiscalQuarterFirstDay extends FormatTimePeriod {

		protected int[] _aFiscalQuarters;
		protected int[] _aFiscalQuarterFirstMonth;

		public GetFiscalQuarterFirstDay(int[] aFiscalQuarters, int[] aFiscalQuarterFirstMonth) {
			super();
			_aFiscalQuarters = aFiscalQuarters;
			_aFiscalQuarterFirstMonth = aFiscalQuarterFirstMonth;
		}

		@Override
		public Object FormatValue(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.MONTH, _aFiscalQuarterFirstMonth[_aFiscalQuarters[_Calendar.get(Calendar.MONTH) - 1] - 1]);
			_Calendar.set(Calendar.DATE, 1);
			_Calendar.clear(Calendar.HOUR_OF_DAY);
			_Calendar.clear(Calendar.AM_PM);
			_Calendar.clear(Calendar.MINUTE);
			_Calendar.clear(Calendar.SECOND);
			_Calendar.clear(Calendar.MILLISECOND);
			return _Calendar.getTime();
		}
	}

	protected class GetFiscalQuarterLastDay extends FormatTimePeriod {

		protected int[] _aFiscalQuarters;
		protected int[] _aFiscalQuarterLastMonth;

		public GetFiscalQuarterLastDay(int[] aFiscalQuarters, int[] aFiscalQuarterLastMonth) {
			super();
			_aFiscalQuarters = aFiscalQuarters;
			_aFiscalQuarterLastMonth = aFiscalQuarterLastMonth;
		}

		@Override
		public Object FormatValue(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.MONTH, _aFiscalQuarterLastMonth[_aFiscalQuarters[_Calendar.get(Calendar.MONTH) - 1] - 1]);
			_Calendar.set(Calendar.DAY_OF_MONTH, 1);
			_Calendar.set(Calendar.HOUR_OF_DAY, 0);
			_Calendar.set(Calendar.MINUTE, 0);
			_Calendar.set(Calendar.SECOND, 0);
			_Calendar.set(Calendar.MILLISECOND, 0);
			_Calendar.add(Calendar.DAY_OF_MONTH, -1);
			return _Calendar.getTime();
		}
	}

	protected class GetHour extends FormatTimePeriod {

		@Override
		public Object FormatValue(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _Calendar.get(Calendar.HOUR);
		}
	}

	protected class GetLastDayOfMonth extends FormatTimePeriod {

		@Override
		public Object FormatValue(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.add(Calendar.MONTH, 1);
			_Calendar.set(Calendar.DAY_OF_MONTH, 1);
			_Calendar.add(Calendar.DATE, -1);
			_Calendar.set(Calendar.HOUR_OF_DAY, 0);
			_Calendar.set(Calendar.MINUTE, 0);
			_Calendar.set(Calendar.SECOND, 0);
			_Calendar.set(Calendar.MILLISECOND, 0);
			return _Calendar.getTime();
		}
	}

	protected class GetLastDayOfQuarter extends FormatTimePeriod {

		protected int[] _aQuarters;
		protected int[] _aQuarterLastMonth;

		public GetLastDayOfQuarter(int[] aQuarters, int[] aQuarterLastMonth) {
			_aQuarters = aQuarters;
			_aQuarterLastMonth = aQuarterLastMonth;
		}

		@Override
		public Object FormatValue(Date dateValue) {
			{
				_Calendar.setTime(dateValue);
				_Calendar.set(Calendar.MONTH, _aQuarterLastMonth[_aQuarters[_Calendar.get(Calendar.MONTH) - 1] - 1]);
				_Calendar.add(Calendar.DATE, -1);
				_Calendar.set(Calendar.HOUR_OF_DAY, 0);
				_Calendar.set(Calendar.MINUTE, 0);
				_Calendar.set(Calendar.SECOND, 0);
				_Calendar.set(Calendar.MILLISECOND, 0);
				return _Calendar.getTime();
			}
		}
	}

	protected class GetLastDayOfWeek extends FormatTimePeriod {

		protected int _iDayOfWeekShift = 0;

		public GetLastDayOfWeek(int iDayOfWeekShift) {
			_iDayOfWeekShift = iDayOfWeekShift;
		}

		@Override
		public Object FormatValue(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.add(Calendar.DATE, (6 - _Calendar.get(Calendar.DAY_OF_WEEK)) + _iDayOfWeekShift);
			_Calendar.set(Calendar.HOUR_OF_DAY, 0);
			_Calendar.set(Calendar.MINUTE, 0);
			_Calendar.set(Calendar.SECOND, 0);
			_Calendar.set(Calendar.MILLISECOND, 0);
			return _Calendar.getTime();
		}

	}

	protected class GetLastDayOfYear extends FormatTimePeriod {

		@Override
		public Object FormatValue(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.MONTH, 12);
			_Calendar.set(Calendar.DAY_OF_MONTH, 31);
			_Calendar.set(Calendar.HOUR_OF_DAY, 23);
			_Calendar.set(Calendar.MINUTE, 59);
			_Calendar.set(Calendar.SECOND, 59);
			_Calendar.set(Calendar.MILLISECOND, 999);
			return _Calendar.getTime();
		}
	}

	protected class GetLastHourOfDay extends FormatTimePeriod {

		@Override
		public Object FormatValue(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.HOUR_OF_DAY, 23);
			_Calendar.set(Calendar.MINUTE, 59);
			_Calendar.set(Calendar.SECOND, 59);
			_Calendar.set(Calendar.MILLISECOND, 999);
			return _Calendar.getTime();
		}
	}

	protected class GetMinute extends FormatTimePeriod {

		@Override
		public Object FormatValue(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _Calendar.get(Calendar.MINUTE);
		}
	}

	protected class GetMonthAbbrevation extends FormatTimePeriod {

		protected String[] _aMonthAbbreviations;

		public GetMonthAbbrevation(String[] aMonthAbbreviations) {
			_aMonthAbbreviations = aMonthAbbreviations;
		}

		@Override
		public Object FormatValue(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _aMonthAbbreviations[_Calendar.get(Calendar.MONTH) - 1];
		}
	}

	protected class GetMonthName extends FormatTimePeriod {

		protected String[] _aMonthNames;

		public GetMonthName(String[] aMonthNames) {
			_aMonthNames = aMonthNames;
		}

		@Override
		public Object FormatValue(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _aMonthNames[_Calendar.get(Calendar.MONTH) - 1];
		}
	}

	protected class GetMonthNumber extends FormatTimePeriod {

		@Override
		public Object FormatValue(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _Calendar.get(Calendar.MONTH)+1;
		}
	}

	protected class GetQuarter extends FormatTimePeriod {

		protected int[] _aQuarters;

		public GetQuarter(int[] aQuarters) {
			_aQuarters = aQuarters;
		}

		@Override
		public Object FormatValue(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _aQuarters[_Calendar.get(Calendar.MONTH) - 1];
		}
	}

	protected class GetSecond extends FormatTimePeriod {

		@Override
		public Object FormatValue(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _Calendar.get(Calendar.SECOND);
		}
	}

	protected class GetFirstSecondOfMinute extends FormatTimePeriod {

		@Override
		public Object FormatValue(Date dateValue) {
			_Calendar.setTime(dateValue);
			_Calendar.set(Calendar.SECOND, 0);
			return _Calendar.getTime();
		}
	}

	protected class GetWeek extends FormatTimePeriod {

		@Override
		public Object FormatValue(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _Calendar.get(Calendar.WEEK_OF_YEAR);
		}
	}

	protected class GetYear extends FormatTimePeriod {

		@Override
		public Object FormatValue(Date dateValue) {
			_Calendar.setTime(dateValue);
			return _Calendar.get(Calendar.YEAR);
		}
	}

}
