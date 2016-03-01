package com.fanniemae.automation.data.connectors;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

import org.w3c.dom.Element;

import com.fanniemae.automation.SessionManager;
import com.fanniemae.automation.common.DataUtilities;
import com.fanniemae.automation.common.FileUtilities;
import com.fanniemae.automation.common.StringUtilities;
import com.fanniemae.automation.datafiles.lowlevel.DataFileEnums.DataType;
import com.opencsv.CSVReader;

/**
 * 
 * @author Richard Monson
 * @since 2016-01-05
 * 
 */
public class DelimitedConnector extends DataConnector {

	protected CSVReader _reader;

	protected String _Filename;

	protected char _Delimiter = ',';

	protected Boolean _IncludesColumnNames = true;

	protected int _ColumnCount;

	protected Object[] _DataRow;
	protected DataType[] _DataTypes;

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
		scanSchema(_Filename);
	}

	@Override
	public Boolean open() {
		try {
			_reader = new CSVReader(new FileReader(_Filename), ',');
			if (_IncludesColumnNames) {
				_reader.readNext();
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
		String[] dataRow;
		try {
			dataRow = _reader.readNext();
			if (dataRow == null) {
				return true;
			}
			int iLen = dataRow.length < _ColumnCount ? dataRow.length : _ColumnCount;
			// null the previous row values before reading the next row.
			Arrays.fill(_DataRow, null);

			// strongly type the new row values.		
			for (int i = 0; i < iLen; i++) {
				_DataRow[i] = castValue(i, dataRow[i]);
			}
		} catch (IOException ex) {
			throw new RuntimeException("Error while reading delimited data file.", ex);
		}
		return false;
	}

	@Override
	public Object[] getDataRow() {
		return _DataRow;
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

			// Read/create column names.
			_DataSchema = new String[dataRow.length][2];
			_DataRow = new Object[dataRow.length];
			_DataTypes = new DataType[dataRow.length];
			boolean[] skipSchemaCheck = new boolean[dataRow.length];
			for (int i = 0; i < dataRow.length; i++) {
				_DataSchema[i][0] = _IncludesColumnNames ? dataRow[i] : String.format("Column%d", i);
				skipSchemaCheck[i] = false;
			}

			if (_IncludesColumnNames) {
				dataRow = rdr.readNext();
			}

			while (dataRow != null) {
				for (int i = 0; i < Math.min(dataRow.length, _DataSchema.length); i++) {
					if (!skipSchemaCheck[i] && StringUtilities.isNotNullOrEmpty(dataRow[i])) {
						_DataSchema[i][1] = StringUtilities.getDataType(dataRow[i], _DataSchema[i][1]);
						if (StringUtilities.isNotNullOrEmpty(_DataSchema[i][1]) && _DataSchema[i][1].equals("StringData")) {
							skipSchemaCheck[i] = true;
						}
					}
				}
				dataRow = rdr.readNext();
			}

			// Load the data types for each column (faster conversion from
			// string to whatever)
			_ColumnCount = _DataSchema.length;
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < _ColumnCount; i++) {
				_DataTypes[i] = DataUtilities.DataTypeToEnum(_DataSchema[i][1]);
				if (i > 0) {
					sb.append(",\n");
				}
				sb.append(String.format("%s {%s}", (Object[]) _DataSchema[i]));
			}
			_Session.addLogMessage("", "Columns", sb.toString());

		} catch (FileNotFoundException e) {
			throw new RuntimeException(String.format("%s file not found.", _Filename), e);
		} catch (IOException e) {
			throw new RuntimeException(String.format("Could not read schema for delimited file %s", _Filename), e);
		}
	}

	protected Object castValue(int i, String value) {
		if (StringUtilities.isNullOrEmpty(value)) {
			return null;
		}

		switch (_DataTypes[i]) {
		case StringData:
			return value;
		case DateData:
			return StringUtilities.toDate(value);
		case IntegerData:
			return StringUtilities.toInteger(value);
		case LongData:
			return StringUtilities.toLong(value);
		case DoubleData:
			return StringUtilities.toDouble(value);
		case BigDecimalData:
			return StringUtilities.toBigDecimal(value);
		case BooleanData:
			return StringUtilities.toBoolean(value);
		default:
			throw new RuntimeException(String.format("%s string conversion not currently available.", DataType.values()[i]));
		}
	}

}
