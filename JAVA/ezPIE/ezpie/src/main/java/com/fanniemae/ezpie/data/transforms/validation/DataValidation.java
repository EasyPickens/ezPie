package com.fanniemae.ezpie.data.transforms.validation;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.ArrayUtilities;
import com.fanniemae.ezpie.common.DataUtilities;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

public abstract class DataValidation {

	protected SessionManager _session;
	protected Element _transform;
	protected String[][] _inputSchema;

	protected String _dataColumn;
	protected boolean _allowNulls = true;

	protected int _sourceColumnIndex = -1;
	protected DataType _sourceColumnDataType = DataType.StringData;

	protected int _rowNumber = 0;

	public DataValidation(SessionManager session, Element transform, String[][] inputSchema) {

		_session = session;
		_transform = transform;
		_inputSchema = inputSchema;

		_dataColumn = getRequiredAttribute("DataColumn");

		_sourceColumnIndex = ArrayUtilities.indexOf(inputSchema, _dataColumn);
		if (_sourceColumnIndex == -1) {
			throw new PieException(String.format("%s column was not found in the source dataset.", _dataColumn));
		}

		_sourceColumnDataType = DataUtilities.dataTypeToEnum(inputSchema[_sourceColumnIndex][1]);
	}

	public abstract Object[] validateDataRow(Object[] dataRow);

	protected String getoptionalAttribute(String attributeName) {
		return _session.optionalAttribute(_transform, attributeName);
	}

	protected String getOptionalAttribute(String attributeName, String defaultValue) {
		return _session.optionalAttribute(_transform, attributeName, defaultValue);
	}

	protected String getRequiredAttribute(String attributeName) {
		return _session.requiredAttribute(_transform, attributeName);
	}
} 
