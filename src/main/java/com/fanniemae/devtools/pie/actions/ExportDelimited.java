package com.fanniemae.devtools.pie.actions;

import java.io.FileWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.DataStream;
import com.fanniemae.devtools.pie.common.DateUtilities;
import com.fanniemae.devtools.pie.common.StringUtilities;
import com.fanniemae.devtools.pie.common.XmlUtilities;
import com.fanniemae.devtools.pie.datafiles.DataReader;
import com.fanniemae.devtools.pie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Richard Monson
 * @since 2015-01-31
 * 
 */
public class ExportDelimited extends Action {

	protected String _outputFilename;
	protected String _delimiter = "|";
	protected String _dataSetID;

	protected DataStream _dataStream;

	protected int _outputLength;
	protected String[] _outputColumnNames;
	protected int[] _outputColumnIndexes;
	protected DataType[] _outputColumnDataTypes;
	protected boolean _trimSpaces = false;
	protected boolean _roundDoubles = false;
	protected boolean _appendData = false;

	protected boolean _writeColumnNames = true;

	public ExportDelimited(SessionManager session, Element action) {
		super(session, action, false);

		_outputFilename = requiredAttribute("Filename");
		_session.addLogMessage("", "OutputFilename", _outputFilename);

		_delimiter = optionalAttribute("Delimiter", "|");
		_session.addLogMessage("", "Delimiter", _delimiter);

		String trimSpaces = optionalAttribute("TrimSpaces", null);
		_trimSpaces = StringUtilities.toBoolean(trimSpaces, false);
		if (StringUtilities.isNotNullOrEmpty(trimSpaces)) {
			_session.addLogMessage("", "TrimSpaces", _trimSpaces ? "True" : "False");
		}

		String appendData = optionalAttribute("Append", null);
		_appendData = StringUtilities.toBoolean(appendData, false);
		if (StringUtilities.isNotNullOrEmpty(appendData)) {
			_session.addLogMessage("", "Append", _appendData ? "True" : "False");
		}

		String roundDoubles = optionalAttribute("RoundDoubles", null);
		_roundDoubles = StringUtilities.toBoolean(roundDoubles, false);
		if (StringUtilities.isNotNullOrEmpty(roundDoubles)) {
			_session.addLogMessage("", "RoundDoubles", _roundDoubles ? "True" : "False");
		}

		_dataSetID = requiredAttribute("DataSetID");
		_writeColumnNames = StringUtilities.toBoolean(optionalAttribute("IncludeColumnNames", null), true);
	}

	@Override
	public String execute() {
		_dataStream = _session.getDataStream(_dataSetID);

		try (DataReader dr = new DataReader(_dataStream); FileWriter fw = new FileWriter(_outputFilename, _appendData)) {
			defineOutputColumns(dr.getColumnNames());
			_outputColumnDataTypes = dr.getDataTypes();

			if (!_appendData && _writeColumnNames) {
				// Write Column Headers
				for (int i = 0; i < _outputLength; i++) {
					if (i > 0)
						fw.append(',');
					fw.append(wrapString(_outputColumnNames[i]));
				}
				fw.append(System.lineSeparator());
			}

			int iRowCount = 0;
			// Write the data
			while (!dr.eof()) {
				Object[] dataRow = dr.getDataRow();

				for (int i = 0; i < _outputLength; i++) {
					if (i > 0)
						fw.append(',');

					if (_outputColumnDataTypes[_outputColumnIndexes[i]] == DataType.DateData) {
						fw.append(DateUtilities.toIsoString((Date) dataRow[_outputColumnIndexes[i]]));
					} else if (_outputColumnDataTypes[_outputColumnIndexes[i]] == DataType.StringData) {
						fw.append(wrapString(dataRow[_outputColumnIndexes[i]]));
					} else if (_outputColumnDataTypes[_outputColumnIndexes[i]] == DataType.DoubleData && _roundDoubles) {
						fw.append(doubleFormat(dataRow[_outputColumnIndexes[i]]));
					} else if (dataRow[_outputColumnIndexes[i]] == null) {
						fw.append("");
					} else {
						fw.append(dataRow[_outputColumnIndexes[i]].toString());
					}
				}
				fw.append(System.lineSeparator());
				iRowCount++;
			}
			fw.close();
			dr.close();
			_session.addLogMessage("", "Data", String.format("%,d rows of data written.", iRowCount));
			_session.addLogMessage("", "Completed", String.format("Data saved to %s", _outputFilename));
		} catch (Exception e) {
			RuntimeException ex = new RuntimeException("Error while trying to export the data into a delimited file.", e);
			throw ex;
		}

		return _outputFilename;
	}

	protected void defineOutputColumns(String[] fileColumns) {
		List<String> inputColumnNames = Arrays.asList(fileColumns);

		NodeList outputColumnNodes = XmlUtilities.selectNodes(_action, "Column");
		int numberOfOutputColumns = outputColumnNodes.getLength();

		if (numberOfOutputColumns > 0) {
			_outputColumnNames = new String[numberOfOutputColumns];
			_outputColumnIndexes = new int[numberOfOutputColumns];

			for (int i = 0; i < numberOfOutputColumns; i++) {
				Element columnElement = (Element) outputColumnNodes.item(i);

				String inputName = _session.getAttribute(columnElement, "Name");
				String alais = _session.getAttribute(columnElement, "Alias");

				_outputColumnNames[i] = StringUtilities.isNotNullOrEmpty(alais) ? alais : inputName;
				_outputColumnIndexes[i] = inputColumnNames.indexOf(inputName);
			}
		} else {
			numberOfOutputColumns = inputColumnNames.size();
			_outputColumnNames = new String[numberOfOutputColumns];
			_outputColumnIndexes = new int[numberOfOutputColumns];

			for (int i = 0; i < numberOfOutputColumns; i++) {
				_outputColumnNames[i] = inputColumnNames.get(i);
				_outputColumnIndexes[i] = i;
			}
		}
		_outputLength = _outputColumnIndexes.length;
	}

	protected String wrapString(Object objectValue) {
		if (objectValue == null) {
			return "";
		}

		boolean wrapDoubleQuotes = false;
		String value = objectValue.toString();
		if (_trimSpaces) {
			value = value.trim();
		}
		if (StringUtilities.isNullOrEmpty(value)) {
			return "";
		} else if (value.contains("\"")) {
			value = value.replace("\"", "\"\"");
			wrapDoubleQuotes = true;
		}

		if (value.contains(_delimiter)) {
			wrapDoubleQuotes = true;
		}

		if (wrapDoubleQuotes) {
			value = "\"" + value + "\"";
		}
		return value;
	}

	protected String doubleFormat(Object value) {
		if (value == null) {
			return "";
		}
		return String.format("%.2f", (double) value);
	}
}
