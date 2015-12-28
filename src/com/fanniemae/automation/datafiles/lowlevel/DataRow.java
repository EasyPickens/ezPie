package com.fanniemae.automation.datafiles.lowlevel;

import com.fanniemae.automation.common.DataUtilities;
import com.fanniemae.automation.datafiles.lowlevel.DataFileEnums.ColumnTypes;
import com.fanniemae.automation.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-28
 * 
 */
public class DataRow {
	protected String[] _ColumnNames = null;
	protected ColumnTypes[] _ColumnTypes = null;
	protected DataType[] _DataTypes = null;
	protected Object[] _Values = null;

	public DataRow(int nNumberOfColumns) {
		_ColumnNames = new String[nNumberOfColumns];
		_ColumnTypes = new ColumnTypes[nNumberOfColumns];
		_DataTypes = new DataType[nNumberOfColumns];
		_Values = new Object[nNumberOfColumns];
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
		_ColumnNames[nColumnIndex] = sColumnName;
		_ColumnTypes[nColumnIndex] = ColumnType;
		_DataTypes[nColumnIndex] = DataType;
		_Values[nColumnIndex] = oGlobalValue;
	}

	public Object[] getValues() {
		return _Values;
	}

	public void setValues(Object[] values) {
		_Values = values;
	}

	public String[] getColumnNames() {
		return _ColumnNames;
	}

	public DataType[] getDataTypes() {
		return _DataTypes;
	}

	public String getColumnName(int i) {
		return _ColumnNames[i];
	}

	public ColumnTypes getColumnType(int i) {
		return _ColumnTypes[i];
	}

	public Object getValue(int i) {
		return _Values[i];
	}

	public int getColumnCount() {
		return _ColumnNames.length;
	}

	public DataType getDataType(int i) {
		return _DataTypes[i];
	}

	public void setDataType(int i, DataType adjustedDataType) {
		_DataTypes[i] = adjustedDataType;
	}
}
