package com.fanniemae.ezpie.data.transforms.validation;

import java.util.Date;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

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

		Double doubleValue = null;
		if (_sourceColumnDataType == DataType.DoubleData) {
			doubleValue = (Double) objValue;
			validationResults[2] = StringUtilities.formatAsNumber(doubleValue);
		} else if (_sourceColumnDataType == DataType.StringData) {
			// Try to convert the string value into a numeric
			validationResults[2] = (String) objValue;
			Double doubleClass = StringUtilities.toDoubleClass((String) objValue);
			if (doubleClass == null) {
				validationResults[3] = "Provided string could not be converted into a valid numeric value.";
				return validationResults;
			}
			doubleValue = doubleClass.doubleValue();
		}

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

//	private Double readDoubleValue(String string) {
//		Double result = 0.0D;
//		try {
//			result = Double.parseDouble(string);
//		} catch (NumberFormatException ex) {
//			if (StringUtilities.toDouble(string) == 0.0D) {
//				throw new PieException(String.format("%s requires a valid double. '%s' is not a recognized double value or format.", string, result));
//			}
//
//			return 0.0D;
//		}
//		return result;
//	}
}
