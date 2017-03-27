package com.fanniemae.devtools.pie.common;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
		if (_columnCount == 0) {
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

	public void addRow(Object[] data) {
		if (_columnCount == 0) {
			throw new RuntimeException("No datatable schema defined.");
		} else if ((data != null) && (data.length > 0)) {
			Object[] row = new Object[_columnCount];
			System.arraycopy(data, 0, row, 0, Math.min(_columnCount, data.length));
			_dataRows.add(row);
		}
	}

	public boolean endOfData() {
		return _currentRow < _dataRows.size();
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

	public Object[] getValues(int i) {
		if ((i == 0) || (_dataRows.size() < i)) {
			return null;
		}
		return _dataRows.get(i);
	}
}
