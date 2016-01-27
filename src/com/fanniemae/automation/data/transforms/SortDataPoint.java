package com.fanniemae.automation.data.transforms;

import java.math.BigDecimal;
import java.util.Date;

import com.fanniemae.automation.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Richard Monson
 * @since 2016-01-21
 * 
 *        A single data point from one row of data. See SortKeys class for the
 *        rest of the data row.
 * 
 */
class SortDataPoint implements Comparable<SortDataPoint> {
	protected DataType _DataType;
	protected boolean _isAscending = true;
	protected int _directionModifier = 1;

	protected String _s;
	protected int _i;
	protected long _l;
	protected double _d;
	protected BigDecimal _bd;
	protected boolean _b;
	protected Date _dt;

	public SortDataPoint(String value) {
		this(value, false);
	}

	public SortDataPoint(int value) {
		this(value, false);
	}

	public SortDataPoint(long value) {
		this(value, false);
	}

	public SortDataPoint(double value) {
		this(value, false);
	}

	public SortDataPoint(BigDecimal value) {
		this(value, false);
	}

	public SortDataPoint(boolean value) {
		this(value, false);
	}

	public SortDataPoint(Date value) {
		this(value, false);
	}

	public SortDataPoint(String value, boolean isAscending) {
		_DataType = DataType.StringData;
		_isAscending = isAscending;
		_s = value;
		updateModifier();
	}

	public SortDataPoint(int value, boolean isAscending) {
		_DataType = DataType.IntegerData;
		_isAscending = isAscending;
		_i = value;
		updateModifier();
	}

	public SortDataPoint(long value, boolean isAscending) {
		_DataType = DataType.LongData;
		_isAscending = isAscending;
		_l = value;
		updateModifier();
	}

	public SortDataPoint(double value, boolean isAscending) {
		_DataType = DataType.DoubleData;
		_isAscending = isAscending;
		_d = value;
		updateModifier();
	}

	public SortDataPoint(BigDecimal value, boolean isAscending) {
		_DataType = DataType.BigDecimalData;
		_isAscending = isAscending;
		_bd = value;
		updateModifier();
	}

	public SortDataPoint(boolean value, boolean isAscending) {
		_DataType = DataType.BooleanData;
		_isAscending = isAscending;
		_b = value;
		updateModifier();
	}

	public SortDataPoint(Date value, boolean isAscending) {
		_DataType = DataType.DateData;
		_isAscending = isAscending;
		_dt = value;
		updateModifier();
	}

	public Object getValue() {
		switch (_DataType) {
		case StringData:
			return _s;
		case IntegerData:
			return _i;
		case LongData:
			return _l;
		case DoubleData:
			return _d;
		case BigDecimalData:
			return _bd;
		case BooleanData:
			return _b;
		case DateData:
			return _dt;
		default:
			throw new RuntimeException("SortKey does not contain a value of the defined types.");
		}
	}
	
	protected void updateModifier() {
		_directionModifier = _isAscending ? 1 : -1;
	}

	@Override
	public int compareTo(SortDataPoint o) {
		switch (_DataType) {
		case StringData:
			if ((this._s == null) && (o._s == null)) {
				return 0;
			} else if ((this._s == null)) {
				return -1 * this._directionModifier;
			}
			return this._s.compareTo(o._s) * this._directionModifier;
		case IntegerData:
			return (this._i > o._i ? 1 : (this._i < o._i) ? -1 : 0) * this._directionModifier;
		case LongData:
			return (this._l > o._l ? 1 : (this._l < o._l) ? -1 : 0) * this._directionModifier;
		case DoubleData:
			return (this._d > o._d ? 1 : (this._d < o._d) ? -1 : 0) * this._directionModifier;
		case BigDecimalData:
			if ((this._bd == null) && (o._bd == null)) {
				return 0;
			} else if (this._bd == null) {
				return -1 * this._directionModifier;
			}
			return this._bd.compareTo(o._bd) * this._directionModifier;
		case BooleanData:
			return (this._b == o._b ? 0 : (this._b ? 1 : -1)) * this._directionModifier;
		case DateData:
			if ((this._dt == null) && (o._dt == null)) {
				return 0;
			} else if (this._dt == null) {
				return -1 * this._directionModifier;
			}
			return _dt.compareTo(o._dt) * this._directionModifier;
		default:
			return 0;
		}
	}
}
