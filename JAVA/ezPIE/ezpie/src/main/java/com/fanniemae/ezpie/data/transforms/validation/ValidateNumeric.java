package com.fanniemae.ezpie.data.transforms.validation;

import java.util.Date;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.DataUtilities;
import com.fanniemae.ezpie.common.StringUtilities;

/**
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2018-09-15
 * 
 */

public class ValidateNumeric extends DataValidation {

	protected Double _minValue = null;
	protected Double _maxValue = null;
	protected boolean _allowDecimals = true;

	public ValidateNumeric(SessionManager session, Element transform, String[][] inputSchema) {
		super(session, transform, inputSchema);

		_minValue = StringUtilities.toDoubleClass(getOptionalAttribute("MinValue"));
		_maxValue = StringUtilities.toDoubleClass(getOptionalAttribute("MaxValue"));
		_allowNulls = StringUtilities.toBoolean(getOptionalAttribute("AllowNulls"), false);
		_allowDecimals = StringUtilities.toBoolean(getOptionalAttribute("AllowDecimals"), true);
	}

	@Override
	public Object[] validateDataRow(Object[] dataRow) {
		Object[] validationResults = new Object[] { _rowNumber, _dataColumn, "null", "Missing required valid numeric value.", new Date() };
		_rowNumber++;

		Object objValue = dataRow[_sourceColumnIndex];
		if ((objValue == null) && !_allowNulls) {
			validationResults[3] = "No numeric value provided.";
			return validationResults;
		} else if (objValue == null) {
			return null;
		}

		Double doubleValue = DataUtilities.toDoubleClass(objValue);
		if (doubleValue == null) {
			validationResults[3] = "Provided value could not be converted into a numeric value.";
			validationResults[2] = objValue.toString();
			return validationResults;
		}
		
		validationResults[2] = StringUtilities.formatAsNumber(doubleValue);

		if (!_allowDecimals && (Math.floor(doubleValue) != doubleValue)) {
			validationResults[3] = "Provided numeric is not an integer.";
			return validationResults;
		} else if ((_minValue != null) && (doubleValue < _minValue)) {
			validationResults[3] = String.format("Provided numeric is less than the set minimum value of %s.", StringUtilities.formatAsNumber(_minValue));
			return validationResults;
		} else if ((_maxValue != null) && (doubleValue > _maxValue)) {
			validationResults[3] = String.format("Provided numeric is greater than the set maximum value of %s.", StringUtilities.formatAsNumber(_maxValue));
			return validationResults;
		}

		return null;
	}

}
