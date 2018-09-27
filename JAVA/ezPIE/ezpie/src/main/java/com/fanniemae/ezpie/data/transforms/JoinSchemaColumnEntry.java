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

import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2016-02-12
 * 
*/

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
