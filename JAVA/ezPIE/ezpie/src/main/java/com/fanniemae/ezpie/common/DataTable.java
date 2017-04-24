/**
 *  
 * Copyright (c) 2017 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPIE/wiki/Fannie-Mae-Open-Source-Licensing-Project
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.common;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2017-03-27
 * 
 */

public class DataTable {

	protected String[] _columnNames = new String[] {};
	protected Class<?>[] _columnTypes = new Class<?>[] {};
	protected List<Object[]> _dataRows = new ArrayList<Object[]>();

	protected int _columnCount = 0;
	protected int _currentRow = 0;

	public DataTable() {
	}

	public DataTable(String[][] schema) {
		setSchema(schema);
	}

	public void setSchema(String[][] schema) {
		if (_columnCount > 0) {
			throw new RuntimeException("Datatable schema already defined.");
		} else if ((schema != null) && (schema.length > 0)) {
			_columnNames = new String[schema.length];
			_columnTypes = new Class<?>[schema.length];
			for (int i = 0; i < schema.length; i++) {
				_columnNames[i] = schema[i][0];
				_columnTypes[i] = DataUtilities.StringNameToJavaType(schema[i][1]);
			}
			_columnCount = _columnNames.length;
		}
	}
	
	public int getColumnCount() {
		return _columnCount;
	}
	
	public int getRowCount() {
		return _dataRows.size();
	}
	
	public boolean containsColumn(String name) {
		return ArrayUtilities.contains(_columnNames, name);
	}

	public void addRow(Object[] data) {
		if (_columnCount == 0) {
			throw new RuntimeException("No datatable schema defined.");
		} else if ((data != null) && (data.length > 0)) {
			Object[] row = new Object[_columnCount];
			System.arraycopy(data, 0, row, 0, Math.min(_columnCount, data.length));
			_dataRows.add(row);
		}
	}
	
	public void gotoStart() {
		_currentRow = 0;
	}

	public boolean endOfData() {
		return _currentRow >= _dataRows.size();
	}

	public void nextRow() {
		_currentRow++;
	}

	public BigDecimal getBigDecimal(String name) {
		Object result = getValue(name);
		return (result == null) ? null : (BigDecimal) result;
	}

	public BigDecimal getBigDecimal(int index) {
		Object result = getValue(index);
		return (result == null) ? null : (BigDecimal) result;
	}

	public boolean getBoolean(String name) {
		Object result = getValue(name);
		return (result == null) ? false : (boolean) result;
	}

	public boolean getBoolean(int index) {
		Object result = getValue(index);
		return (result == null) ? false : (boolean) result;
	}

	public byte getByte(String name) {
		Object result = getValue(name);
		return (result == null) ? null : (Byte) result;
	}

	public byte getByte(int index) {
		Object result = getValue(index);
		return (result == null) ? null : (Byte) result;
	}

	public char getChar(String name) {
		Object result = getValue(name);
		return (result == null) ? null : (char) result;
	}

	public char getChar(int index) {
		Object result = getValue(index);
		return (result == null) ? null : (char) result;
	}

	public Date getDate(String name) {
		Object result = getValue(name);
		return (result == null) ? null : (Date) result;
	}

	public Date getDate(int index) {
		Object result = getValue(index);
		return (result == null) ? null : (Date) result;
	}

	public double getDouble(String name) {
		Object result = getValue(name);
		return (result == null) ? Double.MIN_VALUE : (double) result;
	}

	public double getDouble(int index) {
		Object result = getValue(index);
		return (result == null) ? Double.MIN_VALUE : (double) result;
	}

	public float getFloat(String name) {
		Object result = getValue(name);
		return (result == null) ? Float.MIN_VALUE : (float) result;
	}

	public float getFloat(int index) {
		Object result = getValue(index);
		return (result == null) ? Float.MIN_VALUE : (float) result;
	}

	public int getInteger(String name) {
		Object result = getValue(name);
		return (result == null) ? Integer.MIN_VALUE : (int) result;
	}

	public int getInteger(int index) {
		Object result = getValue(index);
		return (result == null) ? Integer.MIN_VALUE : (int) result;
	}

	public long getLong(String name) {
		Object result = getValue(name);
		return (result == null) ? Long.MIN_VALUE : (long) result;
	}

	public long getLong(int index) {
		Object result = getValue(index);
		return (result == null) ? Long.MIN_VALUE : (long) result;
	}

	public short getShort(String name) {
		Object result = getValue(name);
		return (result == null) ? Short.MIN_VALUE : (short) result;
	}

	public short getShort(int index) {
		Object result = getValue(index);
		return (result == null) ? Short.MIN_VALUE : (short) result;
	}

	public String getString(String name) {
		Object result = getValue(name);
		return (result == null) ? null : (String) result;
	}

	public String getString(int index) {
		Object result = getValue(index);
		return (result == null) ? null : (String) result;
	}

	public Timestamp getTimestamp(String name) {
		Object result = getValue(name);
		return (result == null) ? null : (Timestamp) result;
	}

	public Timestamp getTimestamp(int index) {
		Object result = getValue(index);
		return (result == null) ? null : (Timestamp) result;
	}

	public Object getValue(String name) {
		if ((name == null) || ArrayUtilities.notContains(_columnNames, name)) {
			throw new RuntimeException(String.format("%s column not found in the datatable.", name));
		}
		return getValue(ArrayUtilities.indexOf(_columnNames, name));
	}

	public Object getValue(int index) {
		if ((index < 0) || (index >= _dataRows.get(_currentRow).length)) {
			throw new RuntimeException(String.format("Column %d not found in datatable.", index));
		}
		return _dataRows.get(_currentRow)[index];
	}

	public Object[] getValues() {
		return getValues(_currentRow);
	}

	public Object[] getValues(int rowNumber) {
		if ((rowNumber == 0) || (_dataRows.size() < rowNumber)) {
			return null;
		}
		return _dataRows.get(rowNumber);
	}
	
	public void setValue(String name, Object value) {
		if ((name == null) || ArrayUtilities.notContains(_columnNames, name)) {
			throw new RuntimeException(String.format("%s column not found in the datatable.", name));
		}
		setValue(ArrayUtilities.indexOf(_columnNames, name), value);
	}
	
	public void setValue(int colIndex, Object value) {
		_dataRows.get(_currentRow)[colIndex] = value;
	}
	
	public void addColumn(String columnName, String javaDataType) {
		if (ArrayUtilities.contains(_columnNames, columnName)) {
			throw new RuntimeException(String.format("The data table already contains a column named %s", columnName));
		}
		
		// Resize the arrays and copy the existing contents
		String[] updatedColumnNames = new String[_columnNames.length+1];
		Class<?>[] updatedColumnTypes = new Class<?>[_columnTypes.length+1];
		System.arraycopy(_columnNames, 0, updatedColumnNames, 0, _columnNames.length);
		System.arraycopy(_columnTypes, 0, updatedColumnTypes, 0, _columnTypes.length);
		_columnNames = updatedColumnNames;
		_columnTypes = updatedColumnTypes;
		_columnCount = updatedColumnNames.length;
		// load the new column name and type
		_columnNames[_columnNames.length-1] = columnName;
		_columnTypes[_columnNames.length-1] = DataUtilities.StringNameToJavaType(javaDataType); 
		
		// Update the format for each row, copy existing data, with null for new column value.
		List<Object[]> updatedData = new ArrayList<Object[]>();
		int length = _dataRows.size();
		for (int i=0;i<length;i++) {
			Object[] row = new Object[_columnCount];
			System.arraycopy(_dataRows.get(i), 0, row, 0, Math.min(_columnCount, _dataRows.get(i).length));
			updatedData.add(row);
		}
		_dataRows = updatedData;
	}
}
