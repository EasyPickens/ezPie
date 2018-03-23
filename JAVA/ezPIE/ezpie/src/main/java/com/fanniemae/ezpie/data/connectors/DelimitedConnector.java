/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
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
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-01-14
 * 
 */

public class DelimitedConnector extends DataConnector {

	protected CSVReader _reader;

	protected String _filename;

	protected char _delimiter = ',';

	protected Boolean _includesColumnNames = true;

	protected int _columnCount;

	protected int[] _sourceIndex;
	protected Object[] _dataRow;
	protected DataType[] _dataTypes;

	public DelimitedConnector(SessionManager session, Element dataSource, Boolean isSchemaOnly) {
		super(session, dataSource, isSchemaOnly);

		_filename = _session.getAttribute(_dataSource, "Filename");
		if (StringUtilities.isNullOrEmpty(_filename)) {
			throw new RuntimeException("DataSource.Delimited requires a Filename.");
		} else if (FileUtilities.isInvalidFile(_filename)) {
			throw new PieException(String.format("%s file not found.", _filename));
		}
		_session.addLogMessage("", "Filename", _filename);

		String sDelimiter = _session.getAttribute(_dataSource, "Delimiter");
		if (StringUtilities.isNotNullOrEmpty(sDelimiter)) {
			_delimiter = sDelimiter.charAt(0);
			_session.addLogMessage("", "Delimiter", String.valueOf(_delimiter));
		}
		_includesColumnNames = StringUtilities.toBoolean(_session.optionalAttribute(dataSource, "IncludesColumnNames", null), true);
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
			dataRow = _reader.readNext();
			if (dataRow == null) {
				return true;
			}
			int iLen = Math.min(dataRow.length, _columnCount);
			// null the previous row values before reading the next row.
			Arrays.fill(_dataRow, null);

			// strongly type the new row values.
			for (int i = 0; i < iLen; i++) {
				_dataRow[i] = castValue(i, dataRow[i]);
			}

			// strongly type the new row values.
			for (int i = 0; i < iLen; i++) {
				if (_sourceIndex[i] == -1) {
					_dataRow[i] = null;
				} else {
					_dataRow[i] = castValue(i, dataRow[_sourceIndex[i]]);
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
			boolean[] skipSchemaCheck = new boolean[dataRow.length];
			for (int i = 0; i < dataRow.length; i++) {
				_dataSchema[i][0] = _includesColumnNames ? dataRow[i] : String.format("Column%d", i);
				_sourceIndex[i] = i;
				skipSchemaCheck[i] = false;
			}

			if (_includesColumnNames) {
				dataRow = rdr.readNext();
			}

			while (dataRow != null) {
				for (int i = 0; i < Math.min(dataRow.length, _dataSchema.length); i++) {
					if (!skipSchemaCheck[i] && StringUtilities.isNotNullOrEmpty(dataRow[i])) {
						_dataSchema[i][1] = StringUtilities.getDataType(dataRow[i], _dataSchema[i][1]);
						if (StringUtilities.isNotNullOrEmpty(_dataSchema[i][1]) && "StringData".equals(_dataSchema[i][1])) {
							skipSchemaCheck[i] = true;
						}
					}
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

	protected Object castValue(int i, String value) {
		if (StringUtilities.isNullOrEmpty(value)) {
			return null;
		}

		switch (_dataTypes[i]) {
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
			throw new PieException(String.format("%s string conversion not currently available.", DataType.values()[i]));
		}
	}

	protected void selectedColumns() {
		NodeList outputColumns = XmlUtilities.selectNodes(_dataSource, "Column");
		if (outputColumns.getLength() == 0) {
			return;
		}

		int columnCount = outputColumns.getLength();
		String[][] dataSchema = new String[columnCount][2];
		Object[] dataRow = new Object[columnCount];
		DataType[] dataTypes = new DataType[columnCount];
		_sourceIndex = new int[columnCount];
		for (int i = 0; i < columnCount; i++) {
			Element columnElement = (Element) outputColumns.item(i);

			String inputName = _session.getAttribute(columnElement, "Name");
			String alais = _session.getAttribute(columnElement, "Alias");

			int sourceIndex = ArrayUtilities.indexOf(_dataSchema, inputName);

			dataSchema[i][0] = alais;
			dataSchema[i][1] = _dataSchema[sourceIndex][1];
			dataTypes[i] = _dataTypes[sourceIndex];
			_sourceIndex[i] = sourceIndex;
		}
		_columnCount = dataSchema.length;
		_dataSchema = dataSchema;
		_dataTypes = dataTypes;
		_dataRow = dataRow;
	}
}
