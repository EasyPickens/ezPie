package com.fanniemae.devtools.pie.datafiles.lowlevel;

import com.fanniemae.devtools.pie.common.DataUtilities;
import com.fanniemae.devtools.pie.datafiles.lowlevel.DataFileEnums.ColumnTypes;
import com.fanniemae.devtools.pie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-28
 * 
 */
public class DataRow {
	protected String[] _columnNames = null;
	protected ColumnTypes[] _columnTypes = null;
	protected DataType[] _dataTypes = null;
	protected Object[] _values = null;

	public DataRow(int nNumberOfColumns) {
		_columnNames = new String[nNumberOfColumns];
		_columnTypes = new ColumnTypes[nNumberOfColumns];
		_dataTypes = new DataType[nNumberOfColumns];
		_values = new Object[nNumberOfColumns];
	}

	public void DefineColumn(int nColumnIndex, String sColumnName, String sDataType) {
		DefineColumn(nColumnIndex, sColumnName, ColumnTypes.DataValue, DataUtilities.DataTypeToEnum(sDataType), null);
	}

	public void DefineColumn(int nColumnIndex, String sColumnName, DataType DataType) {
		DefineColumn(nColumnIndex, sColumnName, ColumnTypes.DataValue, DataType, null);
	}

	public void DefineColumn(int nColumnIndex, String sColumnName, ColumnTypes ColumnType, String sDataType, Object oGlobalValue) {
		DefineColumn(nColumnIndex, sColumnName, ColumnType, DataUtilities.DataTypeToEnum(sDataType), oGlobalValue);
	}

	public void DefineColumn(int nColumnIndex, String sColumnName, ColumnTypes ColumnType, DataType DataType, Object oGlobalValue) {
		_columnNames[nColumnIndex] = sColumnName;
		_columnTypes[nColumnIndex] = ColumnType;
		_dataTypes[nColumnIndex] = DataType;
		_values[nColumnIndex] = oGlobalValue;
	}

	public Object[] getValues() {
		return _values;
	}

	public void setValues(Object[] values) {
		_values = values;
	}

	public String[] getColumnNames() {
		return _columnNames;
	}

	public DataType[] getDataTypes() {
		return _dataTypes;
	}

	public String getColumnName(int i) {
		return _columnNames[i];
	}

	public ColumnTypes getColumnType(int i) {
		return _columnTypes[i];
	}

	public Object getValue(int i) {
		return _values[i];
	}

	public int getColumnCount() {
		return _columnNames.length;
	}

	public DataType getDataType(int i) {
		return _dataTypes[i];
	}

	public void setDataType(int i, DataType adjustedDataType) {
		_dataTypes[i] = adjustedDataType;
	}
}
