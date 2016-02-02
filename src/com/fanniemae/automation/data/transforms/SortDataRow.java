package com.fanniemae.automation.data.transforms;

import java.math.BigDecimal;
import java.util.Date;

import com.fanniemae.automation.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Richard Monson
 * @since 2016-01-21
 * 
 *        One row of data (SortKey) used to sort. Each entry is compared until a
 *        greater than or less than is reached. If both rows are equal, the
 *        position (unique long) is checked and first row remains first. By
 *        using the file position the sort becomes a "stable sort".
 * 
 */
class SortDataRow implements Comparable<SortDataRow> {
	protected long _rowStart;
	protected SortDataPoint[] _dataRowKeys;

	public SortDataRow(long rowStart, int keyCount) {
		_rowStart = rowStart;
		_dataRowKeys = new SortDataPoint[keyCount + 1];
		_dataRowKeys[keyCount] = new SortDataPoint(rowStart, true);
	}

	public void setDataPoint(int index, Object value, DataType dataType, boolean isAscending) {
		switch (dataType) {
		case StringData:
			_dataRowKeys[index] = new SortDataPoint((String) value, isAscending);
			break;
		case IntegerData:
			_dataRowKeys[index] = new SortDataPoint((int) value, isAscending);
			break;
		case LongData:
			_dataRowKeys[index] = new SortDataPoint((long) value, isAscending);
			break;
		case DoubleData:
			_dataRowKeys[index] = new SortDataPoint((double) value, isAscending);
			break;
		case BigDecimalData:
			_dataRowKeys[index] = new SortDataPoint((BigDecimal) value, isAscending);
			break;
		case BooleanData:
			_dataRowKeys[index] = new SortDataPoint((boolean) value, isAscending);
			break;
		case DateData:
			_dataRowKeys[index] = new SortDataPoint((Date) value, isAscending);
			break;
		default:
			throw new RuntimeException("SortKey does not contain a conversion for the defined type.");
		}
	}

	public long getRowStart() {
		return _rowStart;
	}

	public Object[] getSortValues() {
		if (_dataRowKeys == null) {
			return null;
		}

		Object[] values = new Object[_dataRowKeys.length];
		for (int i = 0; i < _dataRowKeys.length; i++) {
			values[i] = _dataRowKeys[i].getValue();
		}
		return values;
	}

	public String getSortValuesAsCSV() {
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
	public int compareTo(SortDataRow o) {
		for (int i = 0; i < this._dataRowKeys.length; i++) {
			int result = this._dataRowKeys[i].compareTo(o._dataRowKeys[i]);
			if (result != 0) {
				return result;
			}
		}
		return 0;
	}
}
