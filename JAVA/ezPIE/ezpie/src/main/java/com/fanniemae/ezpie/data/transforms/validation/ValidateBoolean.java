package com.fanniemae.ezpie.data.transforms.validation;

import java.util.Date;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2018-09-18
 * 
 */

public class ValidateBoolean extends DataValidation {

	protected String _trueValue = null;
	protected String _falseValue = null;

	public ValidateBoolean(SessionManager session, Element transform, String[][] inputSchema) {
		super(session, transform, inputSchema);

		_trueValue = getOptionalAttribute("TrueValue");
		_falseValue = getOptionalAttribute("FalseValue");
		_allowNulls = StringUtilities.toBoolean(getOptionalAttribute("AllowNulls"), _allowNulls);

		if (((_trueValue == null) && (_falseValue != null)) || ((_trueValue != null) && (_falseValue == null))) {
			throw new PieException("If a value is assigned to one of the attributes TrueValue or FalseValue, both must be assigned values.");
		}
	}

	@Override
	public Object[] validateDataRow(Object[] dataRow) {
		Object[] validationResults = new Object[] { _rowNumber, _dataColumn, "null", "Missing required boolean value", new Date() };
		_rowNumber++;

		Object objValue = dataRow[_sourceColumnIndex];
		if ((objValue == null) && !_allowNulls) {
			validationResults[3] = "No boolean value provided.";
			return validationResults;
		} else if (objValue == null) {
			return null;
		}

		String value;
		if (_sourceColumnDataType != DataType.StringData) {
			// Try to convert the value to a string
			value = objValue.toString();
		} else {
			value = (String) objValue;
		}

		validationResults[2] = value;

		if ((_trueValue != null) && _trueValue.equals(value)) {
			return null;
		} else if ((_falseValue != null) && _falseValue.equals(value)) {
			return null;
		} else if ((_trueValue == null) && (value != null) && StringUtilities.toBoolean(value)) {
			return null;
		} else {
			validationResults[3] = "Provided value is not a recognized boolean value.";
			return validationResults;
		}

	}

}
