package com.fanniemae.devtools.pie.data.transforms;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.fanniemae.devtools.pie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Richard Monson
 * @since 2016-01-21
 * 
 *        A single data point from one row of data. See IndexKeys class for the
 *        rest of the data row.
 * 
 */
class IndexDataPoint implements Comparable<IndexDataPoint> {
	protected DataType _dataType;
	protected boolean _isAscending = true;
	protected int _directionModifier = 1;

	protected String _s;
	protected int _i;
	protected long _l;
	protected double _d;
	protected BigDecimal _bd;
	protected boolean _b;
	protected Date _dt;

	public IndexDataPoint(String value) {
		this(value, false);
	}

	public IndexDataPoint(int value) {
		this(value, false);
	}

	public IndexDataPoint(long value) {
		this(value, false);
	}

	public IndexDataPoint(double value) {
		this(value, false);
	}

	public IndexDataPoint(BigDecimal value) {
		this(value, false);
	}

	public IndexDataPoint(boolean value) {
		this(value, false);
	}

	public IndexDataPoint(Date value) {
		this(value, false);
	}

	public IndexDataPoint(String value, boolean isAscending) {
		_dataType = DataType.StringData;
		_isAscending = isAscending;
		_s = value;
		updateModifier();
	}

	public IndexDataPoint(int value, boolean isAscending) {
		_dataType = DataType.IntegerData;
		_isAscending = isAscending;
		_i = value;
		updateModifier();
	}

	public IndexDataPoint(long value, boolean isAscending) {
		_dataType = DataType.LongData;
		_isAscending = isAscending;
		_l = value;
		updateModifier();
	}

	public IndexDataPoint(double value, boolean isAscending) {
		_dataType = DataType.DoubleData;
		_isAscending = isAscending;
		_d = value;
		updateModifier();
	}

	public IndexDataPoint(BigDecimal value, boolean isAscending) {
		_dataType = DataType.BigDecimalData;
		_isAscending = isAscending;
		_bd = value;
		updateModifier();
	}

	public IndexDataPoint(boolean value, boolean isAscending) {
		_dataType = DataType.BooleanData;
		_isAscending = isAscending;
		_b = value;
		updateModifier();
	}

	public IndexDataPoint(Date value, boolean isAscending) {
		_dataType = DataType.DateData;
		_isAscending = isAscending;
		_dt = value;
		updateModifier();
	}

	public Object getValue() {
		switch (_dataType) {
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
			throw new RuntimeException("IndexKey does not contain a value of the defined types.");
		}
	}
	
	public Object getValueAsString() {
		switch (_dataType) {
		case StringData:
			return _s;
		case IntegerData:
			return String.format("%d",_i);
		case LongData:
			return String.format("%d",_l);
		case DoubleData:
			return String.format("%d",_d);
		case BigDecimalData:
			return _bd.toPlainString();
		case BooleanData:
			return _b ? "true" : "false";
		case DateData:
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			return (_dt == null) ? "" : df.format(_dt);
		default:
			throw new RuntimeException("IndexKey does not contain a value of the defined types.");
		}
	}
	
	protected void updateModifier() {
		_directionModifier = _isAscending ? 1 : -1;
	}

	@Override
	public int compareTo(IndexDataPoint o) {
		switch (_dataType) {
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
