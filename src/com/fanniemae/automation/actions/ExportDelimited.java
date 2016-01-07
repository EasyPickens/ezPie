package com.fanniemae.automation.actions;

import java.io.FileWriter;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.automation.SessionManager;
import com.fanniemae.automation.common.DataStream;
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

	protected Boolean _IncludeColumnNames = true;

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
		_IncludeColumnNames = StringUtilities.toBoolean(_Session.getAttribute(action, "IncludeColumnNames"), true);
	}

	@Override
	public String execute() {

		try (DataReader dr = new DataReader(_DataStream); FileWriter fw = new FileWriter(_OutputFilename)) {
			defineOutputColumns(dr.getColumnNames());
			_OutputColumnDataTypes = dr.getDataTypes();

			if (_IncludeColumnNames) {
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
				Object[] values = dr.getRowValues();
				for (int i = 0; i < _OutputLength; i++) {
					if (i > 0)
						fw.append(',');

					if (_OutputColumnDataTypes[_OutputColumnIndexes[i]] == DataType.StringData) {
						fw.append(wrapString(values[_OutputColumnIndexes[i]].toString()));
					} else {
						fw.append(values[_OutputColumnIndexes[i]].toString());
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
		List<String> aFileColumns = Arrays.asList(fileColumns);

		NodeList nlOutputColumns = XmlUtilities.selectNodes(_Action, "Column");
		int iLen = nlOutputColumns.getLength();

		if (iLen > 0) {
			_OutputColumnNames = new String[iLen];
			_OutputColumnIndexes = new int[iLen];

			for (int i = 0; i < iLen; i++) {
				Element eleColumn = (Element) nlOutputColumns.item(i);

				String sName = _Session.getAttribute(eleColumn, "Name");
				String sAlias = _Session.getAttribute(eleColumn, "Alias");

				_OutputColumnNames[i] = StringUtilities.isNotNullOrEmpty(sAlias) ? sAlias : sName;
				_OutputColumnIndexes[i] = aFileColumns.indexOf(sName);
			}
		} else {
			iLen = aFileColumns.size();
			_OutputColumnNames = new String[iLen];
			_OutputColumnIndexes = new int[iLen];

			for (int i = 0; i < iLen; i++) {
				_OutputColumnNames[i] = aFileColumns.get(i);
				_OutputColumnIndexes[i] = i;
			}
		}
		_OutputLength = _OutputColumnIndexes.length;
	}

	protected String wrapString(String value) {
		Boolean wrapDoubleQuotes = false;
		if (value.contains("\"")) {
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
