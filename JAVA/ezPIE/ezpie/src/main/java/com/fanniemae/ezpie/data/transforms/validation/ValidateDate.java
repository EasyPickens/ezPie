package com.fanniemae.ezpie.data.transforms.validation;

import java.util.Date;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.DateUtilities;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2018-09-15
 * 
 */

public class ValidateDate extends DataValidation {

	protected Date _minValue = null;
	protected Date _maxValue = null;

	protected String _requiredFormat = null;

	public ValidateDate(SessionManager session, Element transform, String[][] inputSchema) {
		super(session, transform, inputSchema);

		_minValue = readDateValue("MinValue");
		_maxValue = readDateValue("MaxValue");
		_allowNulls = StringUtilities.toBoolean(getOptionalAttribute("AllowNulls", "True"), true);
		_requiredFormat = getOptionalAttribute("RequiredFormat", null);
	}

	@Override
	public Object[] validateDataRow(Object[] dataRow) {
		Object[] validationResults = new Object[] { _rowNumber, _dataColumn, "null", "Missing required valid date value.", new Date() };
		_rowNumber++;
		
		Object objValue = dataRow[_sourceColumnIndex];
		if ((objValue == null) && !_allowNulls) {
			validationResults[3] = "No date value provided.";
			return validationResults;
		} else if (objValue == null) {
			return null;
		}
		
		Date dateValue = null;
		if (_sourceColumnDataType == DataType.DateData) {
			dateValue = (Date) objValue;
		} else if (_sourceColumnDataType == DataType.StringData) {
			// Try to convert the value into a date
			if (_requiredFormat != null) {
				dateValue = StringUtilities.toDate((String) objValue, null, _requiredFormat);
			} else {
				dateValue = StringUtilities.toDate((String)objValue);
			}
			validationResults[2] = (String)objValue;
			
			if (dateValue == null) {
				// String value could not be converted into a valid date.
				validationResults[3] = "Provided string could not be converted into a valid date.";
				return validationResults;
			}
		}
		
		if (dateValue != null) {
			if ((_minValue != null) && dateValue.before(_minValue)) {
				validationResults[3] = String.format("Provided date is before the set minimum date of %s.", DateUtilities.toIsoString(_minValue));
				return validationResults;
			} else if ((_maxValue != null) && dateValue.after(_maxValue)) {
				validationResults[3] = String.format("Provided date is after the set maximum date of %s.", DateUtilities.toIsoString(_maxValue));
				return validationResults;
			}
		}
		
		return null;
	}

	protected Date readDateValue(String attributeName) {
		String value = getOptionalAttribute(attributeName, null);
		if (value == null) {
			return null;
		}

		switch (value.toLowerCase()) {
		case "todaystartofday":
			return DateUtilities.getTodayStartOfDay();
		case "tomorrowstartofday":
			return DateUtilities.getTomorrowStartOfDay();
		case "yesterdaystartofday":
			return DateUtilities.getYesterdayStartOfDay();
		case "todayendofday":
			return DateUtilities.getTodayEndOfDay();
		case "tomorrowendofday":
			return DateUtilities.getTomorrowEndOfDay();
		case "yesterdayendofday":
			return DateUtilities.getYesterdayEndOfDay();
		default:
			if (!StringUtilities.isDate(value)) {
				throw new PieException(String.format("%s requires a valid date.  '%s' is not a recognized date value or format.", attributeName, value));
			}
			return StringUtilities.toDate(value);
		}
	}

}
