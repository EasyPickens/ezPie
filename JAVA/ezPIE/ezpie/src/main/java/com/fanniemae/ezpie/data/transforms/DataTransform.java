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

package com.fanniemae.ezpie.data.transforms;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.ArrayUtilities;
import com.fanniemae.ezpie.common.DataStream;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.common.ReportBuilder;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.common.XmlUtilities;
import com.fanniemae.ezpie.datafiles.DataReader;
import com.fanniemae.ezpie.datafiles.DataWriter;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-01-06
 * 
 */

public abstract class DataTransform {
	protected SessionManager _session;
	protected Element _transform;

	protected String _name;
	protected String _transformElementName;
	protected String _exceptionDataSetName;
	protected String _exceptionFilename;
	protected String _dataColumn;
	protected String _columnType = "java.lang.String";
	protected String _sourceColumnType;

	protected String[][] _inputSchema;

	protected int _outColumnIndex;
	protected int _sourceColumnIndex;
	protected int _rowsProcessed;
	protected int _rowsRemaining;
	protected int _rowsReturned;
	protected int _rowsRemoved;

	protected Boolean _isolate = false;
	protected Boolean _nameRequired = true;
	protected Boolean _newColumn;
	protected Boolean _addedNewColumn;

	protected ReportBuilder _transformInfo = new ReportBuilder();

	public DataTransform(SessionManager session, String transformName) {
		_session = session;
		_transformElementName = transformName;
	}

	public DataTransform(SessionManager session, Element transform) {
		this(session, transform, true);
	}

	public DataTransform(SessionManager session, Element transform, boolean nameRequired) {
		_session = session;
		_transform = transform;
		_nameRequired = nameRequired;

		_transformElementName = transform.getNodeName();
		String type = transform.getAttribute("Type");
		if (StringUtilities.isNotNullOrEmpty(type)) {
			_transformElementName += "." + type;
		}

		_name = (_nameRequired) ? getRequiredAttribute("Name") : getOptionalAttribute("Name");
		_isolate = StringUtilities.toBoolean(getOptionalAttribute("Isolate", "False"), false);

		_exceptionDataSetName = _transform.getAttribute("ExceptionDataSetName");
		if (StringUtilities.isNotNullOrEmpty(_exceptionDataSetName)) {
			_exceptionFilename = FileUtilities.getDataFilename(_session.getStagingPath(), XmlUtilities.getOuterXml(_transform), _exceptionDataSetName, _session.getTokenizer());
			_transformInfo.appendFormatLine("ExceptionID = %s", _exceptionDataSetName);
			_transformInfo.appendFormatLine("Exception Filename = %s", _exceptionFilename);
		}
	}

	public DataStream processDataStream(DataStream inputStream, int memoryLimit) {
		DataStream outputStream = null;
		String tempFilename = FileUtilities.getRandomFilename(_session.getStagingPath());
		try (DataReader br = new DataReader(inputStream); DataWriter bw = new DataWriter(tempFilename, memoryLimit)) {
			String[][] schema = br.getSchema();
			schema = UpdateSchema(schema);

			bw.setDataColumns(schema);
			int rowCount = 0;
			while (!br.eof()) {
				Object[] dataRow = processDataRow(br.getDataRow());
				if (dataRow != null) {
					bw.writeDataRow(dataRow);
					rowCount++;
				}
			}

			Calendar calendarExpires = Calendar.getInstance();
			if (_session.cachingEnabled())
				calendarExpires.add(Calendar.MINUTE, _session.getCacheMinutes());
			bw.setFullRowCount(rowCount); // dc.getFullRowCount(_lFullRowCount));
			bw.setBufferFirstRow(1); // dc.getBufferFirstRow());
			bw.setBufferLastRow(rowCount); // dc.getBufferLastRow());
			bw.setBufferExpires(calendarExpires.getTime());
			bw.setFullRowCountKnown(true); // dc.getFullRowCountKnown());
			bw.close();
			br.close();
			outputStream = bw.getDataStream();
		} catch (Exception ex) {
			throw new PieException(String.format("Error while running %s data stream transformation.", _transformElementName), ex);

		}
		return outputStream;
	}

	public boolean isolated() {
		return _isolate;
	}

	public abstract Object[] processDataRow(Object[] dataRow);

	public String[][] UpdateSchema(String[][] schema) {
		_inputSchema =  ArrayUtilities.cloneArray(schema);
		_outColumnIndex = ArrayUtilities.indexOf(schema, _name, true);

		if (StringUtilities.isNotNullOrEmpty(_dataColumn)) {
			_sourceColumnIndex = ArrayUtilities.indexOf(schema, _dataColumn, true);
			_sourceColumnType = schema[_sourceColumnIndex][1];
		}

		if (_outColumnIndex != -1) {
			schema[_outColumnIndex][1] = _columnType;
			return schema;
		} else if ((_name != null) && !_name.isEmpty()) {
			int nLength = schema.length;
			String[][] aNewSchema = new String[nLength + 1][2];
			for (int i = 0; i < nLength; i++) {
				aNewSchema[i][0] = schema[i][0];
				aNewSchema[i][1] = schema[i][1];
			}
			_newColumn = true;
			_outColumnIndex = nLength;
			aNewSchema[nLength][0] = _name;
			aNewSchema[nLength][1] = _columnType;
			return aNewSchema;
		}
		return schema;
	}

	protected Object[] addDataColumn(Object[] dataRow) {
		Object[] newDataRow = new Object[dataRow.length + 1];
		System.arraycopy(dataRow, 0, newDataRow, 0, dataRow.length);
		return newDataRow;
	}

	protected String[] resizeColumnArray(String[] columnNames) {
		return resizeColumnArray(columnNames, 1);
	}

	protected String[] resizeColumnArray(String[] columnNames, int newColumnCount) {
		_addedNewColumn = true;
		String[] newColumnNames = new String[columnNames.length + newColumnCount];

		System.arraycopy(columnNames, 0, newColumnNames, 0, columnNames.length);
		return newColumnNames;
	}

	protected DataType[] resizeDataTypeArray(DataType[] dataTypes) {
		return resizeDataTypeArray(dataTypes, 1);
	}

	protected DataType[] resizeDataTypeArray(DataType[] dataTypes, int newColumnCount) {
		DataType[] newDataTypes = new DataType[dataTypes.length + newColumnCount];

		System.arraycopy(dataTypes, 0, newDataTypes, 0, dataTypes.length);
		return newDataTypes;
	}

	public void addTransformLogMessage() {
		_session.addLogMessage("", _transformElementName, _transformInfo.toString());
	}

	public int getRowsProcessed() {
		return _rowsProcessed;
	}

	public int getRowsRemaining() {
		return _rowsRemaining;
	}

	public int getRowsRemoved() {
		return _rowsRemoved;
	}

	public String getID() {
		return _name;
	}

	public String getDataColumn() {
		return _dataColumn;
	}

	public boolean isGlobalValue() {
		return false;
	}

	public boolean isColumnProfile() {
		return false;
	}

	public String getGlobalValue() {
		return "";
	}

	public String getGlobalValueType() {
		return "";
	}

	public List<String[]> getColumnProfile() {
		return new ArrayList<>();
	}

	protected String getOptionalAttribute(String attributeName) {
		return getOptionalAttribute(attributeName, "");
	}

	protected String getOptionalAttribute(String attributeName, String defaultValue) {
		String value = _session.getAttribute(_transform, attributeName);
		if (StringUtilities.isNullOrEmpty(value))
			value = defaultValue;
		else
			_transformInfo.appendFormatLine("%s = %s", attributeName, value);
		return value;
	}

	protected String getRequiredAttribute(String attributeName) {
		String value = _session.getAttribute(_transform, attributeName);
		if (StringUtilities.isNullOrEmpty(value))
			throw new PieException(String.format("No value defined for the %s attrbute of the %s transform elment.", attributeName, _transformElementName));

		_transformInfo.appendFormatLine("%s = %s", attributeName, value);
		return value;
	}

}
