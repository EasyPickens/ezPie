package com.fanniemae.automation.data.connectors;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.w3c.dom.Element;

import com.fanniemae.automation.SessionManager;
import com.fanniemae.automation.common.FileUtilities;
import com.fanniemae.automation.common.StringUtilities;
import com.opencsv.CSVReader;

public class DelimitedConnector extends DataConnector {

	protected CSVReader _reader;

	protected String _Filename;

	protected char _Delimiter = ',';

	protected Boolean _IncludesColumnNames = true;

	public DelimitedConnector(SessionManager session, Element dataSource, Boolean isSchemaOnly) {
		super(session, dataSource, isSchemaOnly);

		_Filename = _Session.getAttribute(_DataSource, "Filename");
		if (StringUtilities.isNullOrEmpty(_Filename)) {
			throw new RuntimeException("DataSource.Delimited requires a Filename.");
		} else if (FileUtilities.isInvalidFile(_Filename)) {
			throw new RuntimeException(String.format("%s file not found.", _Filename));
		}
		_Session.addLogMessage("", "Filename", _Filename);

		String sDelimiter = _Session.getAttribute(_DataSource, "Delimiter");
		if (StringUtilities.isNotNullOrEmpty(sDelimiter)) {
			_Delimiter = sDelimiter.charAt(0);
			_Session.addLogMessage("", "Delimiter", String.valueOf(_Delimiter));
		}

		// String value =
		// "998273i758584843876783786536759862395876.39875484868768648764876487648764876487648764876487644";
		// //value ="2016/02";
		// if (StringUtilities.isDate(value)) {
		// _Session.addLogMessage("", "isDateTime", "Yes");
		// }
		// if (StringUtilities.isLong(value)) {
		// _Session.addLogMessage("", "isLong", "Yes");
		// }
		//
		// if (StringUtilities.isInteger(value)) {
		// _Session.addLogMessage("", "isInteger", "Yes");
		// }
		//
		// if (StringUtilities.isDouble(value)) {
		// _Session.addLogMessage("", "isDouble", "Yes");
		// }

		scanSchema(_Filename);
	}

	@Override
	public Boolean open() {
		try {
			_reader = new CSVReader(new FileReader(_Filename), ',');
			if (_IncludesColumnNames) {
				String[] _ColumnNames = _reader.readNext();

				_DataSchema = new String[_ColumnNames.length][2];
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < _ColumnNames.length; i++) {
					if (i > 0)
						sb.append(",\n");
					sb.append(_ColumnNames[i]);
					_DataSchema[i][0] = _ColumnNames[i];
					_DataSchema[i][1] = "StringData";
				}
				_Session.addLogMessage("", "Column Names", sb.toString());
			}
		} catch (FileNotFoundException ex) {
			throw new RuntimeException("File not found.", ex);
		} catch (IOException ex) {
			throw new RuntimeException("Could not read input file.", ex);
		}

		return true;
	}

	@Override
	public Boolean eof() {
		return true;
	}

	@Override
	public Object[] getDataRow() {
		return null;
	}

	@Override
	public void close() {
		if (_reader != null) {
			try {
				_reader.close();
			} catch (IOException ex) {
				throw new RuntimeException("Could not close delimited reader.", ex);
			}
		}
	}

	protected void scanSchema(String filename) {
		try (CSVReader rdr = new CSVReader(new FileReader(_Filename), ',');) {
			String[] dataRow = rdr.readNext();

			// Read/create column names and pre-populate the column type.
			boolean[] skipSchemaCheck = new boolean[dataRow.length];
			_DataSchema = new String[dataRow.length][2];
			for (int i = 0; i < dataRow.length; i++) {
				_DataSchema[i][0] = _IncludesColumnNames ? dataRow[i] : String.format("Column%d", i);
				skipSchemaCheck[i] = false;
			}

			if (_IncludesColumnNames) {
				dataRow = rdr.readNext();
			}

			while (dataRow != null) {
				for (int i = 0; i < dataRow.length; i++) {
					if (!skipSchemaCheck[i] && StringUtilities.isNotNullOrEmpty(dataRow[i])) {
						_DataSchema[i][1] = StringUtilities.getDataType(dataRow[i], _DataSchema[i][1]);
						if (StringUtilities.isNotNullOrEmpty(_DataSchema[i][1]) && _DataSchema[i][1].equals("StringData")) {
							skipSchemaCheck[i] = true;
						}
					}
				}
				dataRow = rdr.readNext();
			}
		} catch (FileNotFoundException e) {
			throw new RuntimeException(String.format("%s file not found.", _Filename), e);
		} catch (IOException e) {
			throw new RuntimeException(String.format("Could not read schema for delimited file %s", _Filename), e);
		}
	}

}
