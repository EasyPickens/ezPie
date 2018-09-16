package com.fanniemae.ezpie.data.transforms.validation;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2018-09-16
 * 
 */

public class ValidateString extends DataValidation {

	protected Integer _minLength;
	protected Integer _maxLength;
	protected boolean _allowNulls = true;
	protected String _allowCharacters;

	protected Pattern _characterChecker;

	public ValidateString(SessionManager session, Element transform, String[][] inputSchema) {
		super(session, transform, inputSchema);
		_minLength = StringUtilities.toIntegerClass(getOptionalAttribute("MinLength"));
		_maxLength = StringUtilities.toIntegerClass(getOptionalAttribute("MaxLength"));
		_allowNulls = StringUtilities.toBoolean(getOptionalAttribute("AllowNulls"), true);
		_allowCharacters = getOptionalAttribute("AllowCharacters");

		if (StringUtilities.isNotNullOrEmpty(_allowCharacters)) {
			_characterChecker = Pattern.compile(_allowCharacters);
		}
	}

	@Override
	public Object[] validateDataRow(Object[] dataRow) {
		Object[] validationResults = new Object[] { _rowNumber, _dataColumn, "null", "No string value provided.", new Date() };
		_rowNumber++;

		Object objValue = dataRow[_sourceColumnIndex];
		if ((objValue == null) && !_allowNulls) {
			validationResults[3] = "No string value provided.";
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

		if (_characterChecker != null) {
			Matcher characterCheck = this._characterChecker.matcher(value);

			if (!characterCheck.matches()) {
				validationResults[3] = String.format("Provided string contains characters that are not allowed (regex used: %s). ", _allowCharacters);
				return validationResults;
			}
		}

		if ((_minLength != null) && (value.length() < _minLength)) {
			validationResults[3] = String.format("Provided string is shorter than the set minimum length of %s.", StringUtilities.formatAsNumber(_minLength));
			return validationResults;
		} else if ((_maxLength != null) && (value.length() > _maxLength)) {
			validationResults[3] = String.format("Provided string is longer than the set maximum length of %s.", StringUtilities.formatAsNumber(_maxLength));
			return validationResults;
		}

		return null;
	}

}
