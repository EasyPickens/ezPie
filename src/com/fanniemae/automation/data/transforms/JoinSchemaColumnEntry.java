package com.fanniemae.automation.data.transforms;

import com.fanniemae.automation.datafiles.lowlevel.DataFileEnums.DataType;

class JoinSchemaColumnEntry {
	protected String _columnName;
	protected DataType _columnType;
	protected boolean _isRightSide = false;
	protected int _sourceIndex;

	public JoinSchemaColumnEntry(String name, DataType dataType, int sourceIndex) {
		this(name, dataType, false, sourceIndex);
	}

	public JoinSchemaColumnEntry(String name, DataType dataType, boolean rightSide, int sourceIndex) {
		_columnName = name;
		_columnType = dataType;
		_isRightSide = rightSide;
		_sourceIndex = sourceIndex;
	}

	public boolean isRightSide() {
		return _isRightSide;
	}

	public String getColumnName() {
		return _columnName;
	}

	public DataType getColumnType() {
		return _columnType;
	}

	public int getColumnIndex() {
		return _sourceIndex;
	}
}
