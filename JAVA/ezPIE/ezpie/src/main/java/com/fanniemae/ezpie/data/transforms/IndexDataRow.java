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

import java.math.BigDecimal;
import java.util.Date;

import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

//@formatter:off
/**
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2016-01-21
 * 
 * One row of data (IndexDataRow) used to sort. Each entry is compared until a greater than or 
 * less than is reached. If both rows are equal, the position (unique long) is checked and first 
 * row remains first. By using the file position the sort becomes a "stable sort".
 * 
 */
//@formatter:on

class IndexDataRow implements Comparable<IndexDataRow> {
	protected int _streamChannel;
	protected long _rowStart;
	protected IndexDataPoint[] _dataRowKeys;
	protected boolean _used = false;

	public IndexDataRow(long rowStart, int keyCount) {
		this(1, rowStart, keyCount);
	}

	public IndexDataRow(int streamChannel, long rowStart, int keyCount) {
		_streamChannel = streamChannel;
		_rowStart = rowStart;
		_dataRowKeys = new IndexDataPoint[keyCount + 1];
		_dataRowKeys[keyCount] = new IndexDataPoint(rowStart, true);
	}

	public void setDataPoint(int index, Object value, DataType dataType, boolean isAscending) {
		switch (dataType) {
		case StringData:
			_dataRowKeys[index] = new IndexDataPoint((String) value, isAscending);
			break;
		case IntegerData:
			_dataRowKeys[index] = new IndexDataPoint((int) value, isAscending);
			break;
		case LongData:
			_dataRowKeys[index] = new IndexDataPoint((long) value, isAscending);
			break;
		case DoubleData:
			_dataRowKeys[index] = new IndexDataPoint((double) value, isAscending);
			break;
		case BigDecimalData:
			_dataRowKeys[index] = new IndexDataPoint((BigDecimal) value, isAscending);
			break;
		case BooleanData:
			_dataRowKeys[index] = new IndexDataPoint((boolean) value, isAscending);
			break;
		case DateData:
			_dataRowKeys[index] = new IndexDataPoint((Date) value, isAscending);
			break;
		default:
			throw new PieException("IndexKey does not contain a conversion for the defined type.");
		}
	}

	public int getStreamChannel() {
		return _streamChannel;
	}

	public long getRowStart() {
		return _rowStart;
	}

	public boolean haveUsed() {
		return _used;
	}

	public void setUsedFlag(boolean value) {
		_used = value;
	}

	public Object[] getIndexValues() {
		if (_dataRowKeys == null) {
			return null;
		}

		Object[] values = new Object[_dataRowKeys.length];
		for (int i = 0; i < _dataRowKeys.length; i++) {
			values[i] = _dataRowKeys[i].getValue();
		}
		return values;
	}

	public String getIndexValuesAsCSV() {
		if (_dataRowKeys == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < _dataRowKeys.length; i++) {
			if (i > 0)
				sb.append(", ");
			sb.append(_dataRowKeys[i].getValueAsString());
		}
		return sb.toString();
	}

	@Override
	public int compareTo(IndexDataRow o) {
		for (int i = 0; i < this._dataRowKeys.length; i++) {
			int result = this._dataRowKeys[i].compareTo(o._dataRowKeys[i]);
			if (result != 0) {
				return result;
			}
		}
		return 0;
	}

	@Override
	public boolean equals(Object o) {
		return compareTo((IndexDataRow) o) == 0;
	}
	
	@Override
	public int hashCode() {
		return super.hashCode();
	}

	public int compareValues(IndexDataRow o) {
		for (int i = 0; i < this._dataRowKeys.length - 1; i++) {
			int result = this._dataRowKeys[i].compareTo(o._dataRowKeys[i]);
			if (result != 0) {
				return result;
			}
		}
		return 0;
	}
}
