package com.fanniemae.automation.actions;

import java.io.FileWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.automation.SessionManager;
import com.fanniemae.automation.common.DataStream;
import com.fanniemae.automation.common.DateUtilities;
import com.fanniemae.automation.common.StringUtilities;
import com.fanniemae.automation.common.XmlUtilities;
import com.fanniemae.automation.datafiles.DataReader;
import com.fanniemae.automation.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Richard Monson
 * @since 2015-01-31
 * 
 */
public class ExportDelimited extends Action {

	protected String _OutputFilename;
	protected String _Delimiter = "|";
	protected String _DataSetID;

	protected DataStream _DataStream;

	protected int _OutputLength;
	protected String[] _OutputColumnNames;
	protected int[] _OutputColumnIndexes;
	protected DataType[] _OutputColumnDataTypes;

	protected boolean  _WriteColumnNames = true;

	public ExportDelimited(SessionManager session, Element action) {
		super(session, action, false);
		
		_OutputFilename = _Session.getAttribute(action, "Filename");
		if (StringUtilities.isNullOrEmpty(_OutputFilename))
			throw new RuntimeException("Missing required output filename.");
		_Session.addLogMessage("", "OutputFilename", _OutputFilename);

		_Delimiter = _Session.getAttribute(action, "Delimiter", "|");
		_Session.addLogMessage("", "Delimiter", _Delimiter);

		_DataSetID = _Session.getAttribute(action, "DataSetID");
		_DataStream = _Session.getDataStream(_DataSetID);
		_WriteColumnNames = StringUtilities.toBoolean(_Session.getAttribute(action, "IncludeColumnNames"), true);
	}

	@Override
	public String execute() {

		try (DataReader dr = new DataReader(_DataStream); FileWriter fw = new FileWriter(_OutputFilename)) {
			defineOutputColumns(dr.getColumnNames());
			_OutputColumnDataTypes = dr.getDataTypes();

			if (_WriteColumnNames) {
				// Write Column Headers
				for (int i = 0; i < _OutputLength; i++) {
					if (i > 0)
						fw.append(',');
					fw.append(wrapString(_OutputColumnNames[i]));
				}
			}

			int iRowCount = 0;
			// Write the data
			while (!dr.eof()) {
				fw.append(System.lineSeparator());
				Object[] dataRow = dr.getDataRow();
			
				for (int i = 0; i < _OutputLength; i++) {
					if (i > 0)
						fw.append(',');

					if (_OutputColumnDataTypes[_OutputColumnIndexes[i]] == DataType.DateData) {
						fw.append(DateUtilities.toIsoString((Date)dataRow[_OutputColumnIndexes[i]]));
					} else if (_OutputColumnDataTypes[_OutputColumnIndexes[i]] == DataType.StringData) {
						fw.append(wrapString(dataRow[_OutputColumnIndexes[i]].toString()));
					} else if (dataRow[_OutputColumnIndexes[i]] == null) {
						fw.append("");
					} else {
						fw.append(dataRow[_OutputColumnIndexes[i]].toString());
					}
				}
				iRowCount++;
			}
			fw.close();
			dr.close();
			_Session.addLogMessage("", "Data", String.format("%,d rows of data written.", iRowCount));
			_Session.addLogMessage("", "Completed", String.format("Data saved to %s",_OutputFilename));
		} catch (Exception e) {
			RuntimeException ex = new RuntimeException("Error while trying to export the data into a delimited file.", e);
			throw ex;
		}

		return _OutputFilename;
	}

	protected void defineOutputColumns(String[] fileColumns) {
		List<String> inputColumnNames = Arrays.asList(fileColumns);

		NodeList outputColumnNodes = XmlUtilities.selectNodes(_Action, "Column");
		int numberOfOutputColumns = outputColumnNodes.getLength();

		if (numberOfOutputColumns > 0) {
			_OutputColumnNames = new String[numberOfOutputColumns];
			_OutputColumnIndexes = new int[numberOfOutputColumns];

			for (int i = 0; i < numberOfOutputColumns; i++) {
				Element columnElement = (Element) outputColumnNodes.item(i);

				String inputName = _Session.getAttribute(columnElement, "Name");
				String alais = _Session.getAttribute(columnElement, "Alias");

				_OutputColumnNames[i] = StringUtilities.isNotNullOrEmpty(alais) ? alais : inputName;
				_OutputColumnIndexes[i] = inputColumnNames.indexOf(inputName);
			}
		} else {
			numberOfOutputColumns = inputColumnNames.size();
			_OutputColumnNames = new String[numberOfOutputColumns];
			_OutputColumnIndexes = new int[numberOfOutputColumns];

			for (int i = 0; i < numberOfOutputColumns; i++) {
				_OutputColumnNames[i] = inputColumnNames.get(i);
				_OutputColumnIndexes[i] = i;
			}
		}
		_OutputLength = _OutputColumnIndexes.length;
	}

	protected String wrapString(String value) {
		Boolean wrapDoubleQuotes = false;
		if (StringUtilities.isNullOrEmpty(value)) {
			return "";
		} else if (value.contains("\"")) {
			value = value.replace("\"", "\"\"");
			wrapDoubleQuotes = true;
		}

		if (value.contains(_Delimiter)) {
			wrapDoubleQuotes = true;
		}

		if (wrapDoubleQuotes) {
			value = "\"" + value + "\"";
		}
		return value;
	}
}
