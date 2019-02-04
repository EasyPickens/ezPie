/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.data.connectors;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.ArrayUtilities;
import com.fanniemae.ezpie.common.DataUtilities;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.common.XmlUtilities;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;
import com.opencsv.CSVReader;

/**
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2016-01-14
 * 
 */

public class DelimitedConnector extends DataConnector {

	protected CSVReader _reader;

	protected String _filename;

	protected char _delimiter = ',';

	protected boolean _includesColumnNames = true;
	protected boolean _fullSchemaScan = false;

	protected int _columnCount;
	protected int _schemaScanLimit = 1000;

	protected int[] _sourceIndex;
	protected String[] _sourceDateFormat;
	protected Object[] _dataRow;
	protected DataType[] _dataTypes;
	protected NodeList _outputColumns;

	public DelimitedConnector(SessionManager session, Element dataSource, boolean isSchemaOnly) {
		super(session, dataSource, isSchemaOnly);

		_filename = _session.getAttribute(_dataSource, "Filename");
		if (StringUtilities.isNullOrEmpty(_filename)) {
			throw new RuntimeException("DataSource.Delimited requires a Filename.");
		} else if (FileUtilities.isInvalidFile(_filename)) {
			String checkResourceDir = FileUtilities.addDirectory(FileUtilities.addDirectory(_session.getApplicationPath(), "_resources"), _filename);
			if (FileUtilities.isInvalidFile(checkResourceDir)) {
			throw new PieException(String.format("%s file not found.", _filename));
			}
			_filename = checkResourceDir;
		}
		_session.addLogMessage("", "Filename", _filename);

		String sDelimiter = _session.getAttribute(_dataSource, "Delimiter");
		if (StringUtilities.isNotNullOrEmpty(sDelimiter)) {
			_delimiter = sDelimiter.charAt(0);
			_session.addLogMessage("", "Delimiter", String.valueOf(_delimiter));
		}
		_includesColumnNames = StringUtilities.toBoolean(_session.optionalAttribute(dataSource, "IncludesColumnNames"), _includesColumnNames);
		_fullSchemaScan = StringUtilities.toBoolean(_session.optionalAttribute(dataSource, "FullScan"),_fullSchemaScan);
		
		// Check for provided output column list, if found scan only the first 2 rows
		_outputColumns = XmlUtilities.selectNodes(_dataSource, "Column");
		if (_outputColumns.getLength() > 0) {
			_fullSchemaScan = false;
			_schemaScanLimit = 2;
		}
		scanSchema(_filename);
		selectedColumns();
	}

	@Override
	public Boolean open() {
		try {
			_reader = new CSVReader(new FileReader(_filename), _delimiter);
			if (_includesColumnNames) {
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
			if (_rowLimit > 0) {
				return true;
			}
			dataRow = _reader.readNext();
			_rowCount++;
			if (dataRow == null) {
				return true;
			}
			int iLen = Math.min(dataRow.length, _columnCount);
			// null the previous row values before reading the next row.
			Arrays.fill(_dataRow, null);

			// strongly type the new row values.
			for (int i = 0; i < iLen; i++) {
				if (_sourceIndex[i] == -1) {
					_dataRow[i] = null;
				} else {
					_dataRow[i] = castValue(i, dataRow[_sourceIndex[i]].trim(), _sourceDateFormat[_sourceIndex[i]]);
				}
			}
		} catch (IOException ex) {
			throw new RuntimeException("Error while reading delimited data file.", ex);
		}
		return false;
	}

	@Override
	public Object[] getDataRow() {
		return _dataRow;
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
		try (CSVReader rdr = new CSVReader(new FileReader(_filename), ',');) {
			String[] dataRow = rdr.readNext();

			// Read/create column names.
			_dataSchema = new String[dataRow.length][2];
			_dataRow = new Object[dataRow.length];
			_dataTypes = new DataType[dataRow.length];
			_sourceIndex = new int[dataRow.length];
			_sourceDateFormat = new String[dataRow.length];
			boolean[] skipSchemaCheck = new boolean[dataRow.length];
			for (int i = 0; i < dataRow.length; i++) {
				String columnName = String.format("Column%d", i);
				if (_includesColumnNames) {
					String checkColName = dataRow[i];
					if (StringUtilities.isNullOrEmpty(checkColName)) {
						columnName = String.format("Column%d", 1);
					} else {
						columnName = checkColName.trim();
					}
				}
				_dataSchema[i][0] = columnName;
				_sourceIndex[i] = i;
				skipSchemaCheck[i] = false;
			}

			if (_includesColumnNames) {
				dataRow = rdr.readNext();
			}

			// Default the schema scan to 1,000 rows of data.
			int row = 0;
			while (dataRow != null) {
				for (int i = 0; i < Math.min(dataRow.length, _dataSchema.length); i++) {
					if (!skipSchemaCheck[i] && StringUtilities.isNotNullOrEmpty(dataRow[i])) {
						_dataSchema[i][1] = StringUtilities.getDataType(dataRow[i], _dataSchema[i][1]);
						if (StringUtilities.isNotNullOrEmpty(_dataSchema[i][1]) && "StringData".equals(_dataSchema[i][1])) {
							skipSchemaCheck[i] = true;
						}
					}
				}
				row++;
				if ((row > _schemaScanLimit) && !_fullSchemaScan) {
					break;
				}
				dataRow = rdr.readNext();
			}

			// Load the data types for each column (faster conversion from
			// string to whatever)
			_columnCount = _dataSchema.length;
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < _columnCount; i++) {
				_dataTypes[i] = DataUtilities.dataTypeToEnum(_dataSchema[i][1]);
				if (i > 0) {
					sb.append(",\n");
				}
				sb.append(String.format("%s {%s}", (Object[]) _dataSchema[i]));
			}
			_session.addLogMessage("", "Columns", sb.toString());

		} catch (FileNotFoundException e) {
			throw new PieException(String.format("%s file not found.", _filename), e);
		} catch (IOException e) {
			throw new PieException(String.format("Could not read schema for delimited file %s", _filename), e);
		}
	}

	protected Object castValue(int i, String value, String dateFormat) {
		if (StringUtilities.isNullOrEmpty(value)) {
			return null;
		}

		switch (_dataTypes[i]) {
		case StringData:
			return value;
		case DateData:
			return StringUtilities.toDate(value, null, dateFormat);
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
			throw new PieException(String.format("%s string conversion not currently available.", DataType.values()[i]));
		}
	}

	protected void selectedColumns() {
		if (_outputColumns.getLength() == 0) {
			return;
		}

		int columnCount = _outputColumns.getLength();
		String[][] dataSchema = new String[columnCount][2];
		Object[] dataRow = new Object[columnCount];
		DataType[] dataTypes = new DataType[columnCount];
		_sourceIndex = new int[columnCount];
		_sourceDateFormat = new String[_dataSchema.length];
		for (int i = 0; i < columnCount; i++) {
			Element columnElement = (Element) _outputColumns.item(i);

			String inputName = _session.getAttribute(columnElement, "Name");
			String alias = _session.getAttribute(columnElement, "Alias");
			String dataTypeString = _session.getAttribute(columnElement, "DataType");
			String dateFormat = _session.getAttribute(columnElement, "DateFormat");
			DataType columnDataType = null;

			int sourceIndex = ArrayUtilities.indexOf(_dataSchema, inputName);
			
			if ("".equals(alias)) {
				alias = inputName;
			}
			
			if (!"".equals(dataTypeString)) {
				columnDataType = DataUtilities.dataTypeToEnum(dataTypeString);
			}
			
			dataSchema[i][0] = alias;
			dataSchema[i][1] = columnDataType != null ? dataTypeString :  _dataSchema[sourceIndex][1];
			dataTypes[i] = columnDataType != null ? columnDataType : _dataTypes[sourceIndex];
			_sourceIndex[i] = sourceIndex;
			_sourceDateFormat[i] = "".equals(dateFormat) ? null : dateFormat;
		}
		_columnCount = columnCount;
		_dataSchema = dataSchema;
		_dataTypes = dataTypes;
		_dataRow = dataRow;
	}
}
