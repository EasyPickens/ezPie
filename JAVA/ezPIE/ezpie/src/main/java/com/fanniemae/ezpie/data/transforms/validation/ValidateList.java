package com.fanniemae.ezpie.data.transforms.validation;

import java.util.Date;
import java.util.HashSet;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.DataStream;
import com.fanniemae.ezpie.common.DateUtilities;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.datafiles.DataReader;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2018-09-16
 * 
 */

public class ValidateList extends DataValidation {

	protected HashSet<String> _allowedValues = new HashSet<String>();

	protected String _dataSetColumnName;
	protected String _dataSetName;
	protected boolean _allowNulls = true;

	public ValidateList(SessionManager session, Element transform, String[][] inputSchema) {
		super(session, transform, inputSchema);

		_allowNulls = StringUtilities.toBoolean(getOptionalAttribute("AllowNulls"), true);

		_dataSetColumnName = getOptionalAttribute("DataStreamColumnName");
		_dataSetName = getRequiredAttribute("DataStreamName");
		DataStream ds = _session.getDataStream(_dataSetName);

		// Build the list of allowed values from the requested datastream column.
		try (DataReader dr = new DataReader(ds);) {
			String[] columnNames = dr.getColumnNames();
			DataType[] dataTypes = dr.getDataTypes();

			int columnIndex = 0;
			if (_dataSetColumnName == null) {
				// if null then default to the first column of the input stream.
				_dataSetColumnName = columnNames[0];
			} else {
				// get the index of the desired column
				for (int i = 0; i < columnNames.length; i++) {
					if (_dataSetColumnName.equals(columnNames[i])) {
						columnIndex = i;
					}
				}
			}

			while (!dr.eof()) {
				Object[] dataRow = dr.getDataRow();

				String value;
				if (dataRow[columnIndex] == null) {
					continue;
				} else if (dataTypes[columnIndex] == DataType.DateData) {
					value = DateUtilities.toIsoString((Date) dataRow[columnIndex]);
				} else {
					value = dataRow[columnIndex].toString();
				}
				_allowedValues.add(value);
			}
			dr.close();
		} catch (Exception ex) {
			throw new PieException(String.format("Error while building the list of allowed values from the dataset %s. %s", _dataSetName, ex.getMessage()), ex);
		}
	}

	@Override
	public Object[] validateDataRow(Object[] dataRow) {
		Object[] validationResults = new Object[] { _rowNumber, _dataColumn, "null", "No value provided.", new Date() };
		_rowNumber++;

		Object objValue = dataRow[_sourceColumnIndex];
		if ((objValue == null) && !_allowNulls) {
			validationResults[3] = "No value provided.";
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

		if (!_allowedValues.contains(value)) {
			validationResults[3] = "Provided value was not found in the list of allowed values.";
			return validationResults;
		}

		return null;
	}

}
