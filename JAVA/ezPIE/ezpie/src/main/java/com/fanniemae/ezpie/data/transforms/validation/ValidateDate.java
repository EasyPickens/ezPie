package com.fanniemae.ezpie.data.transforms.validation;

import java.util.Date;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.DateUtilities;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.common.StringUtilities;

public class ValidateDate extends DataValidation {

	protected Date _minValue = null;
	protected Date _maxValue = null;

	protected String _requiredFormat = null;

	public ValidateDate(SessionManager session, Element transform, String[][] inputSchema) {
		super(session, transform, inputSchema);

		_minValue = readDateValue("MinValue");
		_maxValue = readDateValue("MaxValue");
		_allowNulls = StringUtilities.toBoolean(_requiredFormat, true);
		_requiredFormat = getOptionalAttribute("RequiredFormat", null);
	}

	@Override
	public Object[] validateDataRow(Object[] dataRow) {
		// TODO Auto-generated method stub
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
				throw new PieException(String.format("%s requires a valid ate.  '%s' is not a recognized date value or format.", attributeName, value));
			}
			return StringUtilities.toDate(value);
		}
	}

}
