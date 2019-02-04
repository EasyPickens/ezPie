package com.fanniemae.ezpie.data.transforms.aggregations;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2018-10-20
 * 
 */

public abstract class Aggregation {
	
	protected Object[] _groupValues;

	protected DataType _dataType = DataType.StringData;
	protected int _dataColumnIndex = -1;
	
	protected String _newColumnName;
	protected DataType _newColumnType;

	protected int _totalCount = 0;
	protected int _countWithoutNulls = 0;
	protected boolean _isFirst = true;

	protected String _stringValue;
	protected BigDecimal _bigdecimalValue;
	protected byte _byteValue;
	protected double _doubleValue;
	protected float _floatValue;
	protected int _intValue;
	protected long _longValue;
	protected short _shortValue;
	protected char _charValue;
	protected Date _dateValue;
	protected boolean _booleanValue;
	protected UUID _uuidValue;
	
	// Used if the type of the result does not match the original column type.
	// E.g. Sum integer becomes a long.
	protected Object _objValue = null;

	public Aggregation(DataType columnDataType, int dataColumnIndex) {
		_dataType = columnDataType;
		_dataColumnIndex = dataColumnIndex;
		_newColumnType = newColumnType();
	}

	public void evaluate(Object value) {
		_totalCount++;
		if (value != null) {
			_countWithoutNulls++;
			switch (_dataType) {
			case StringData:
					eval((String) value);
				break;
			case BigDecimalData:
					eval((BigDecimal) value);
				break;
			case ByteData:
					eval((byte) value);
				break;
			case DoubleData:
					eval((double) value);
				break;
			case FloatData:
					eval((float) value);
				break;
			case IntegerData:
					eval((int) value);
				break;
			case LongData:
					eval((long) value);
				break;
			case ShortData:
					eval((short) value);
				break;
			case CharData:
					eval((char) value);
				break;
			case DateData:
					eval((Date) value);
				break;
			case BooleanData:
					eval((boolean) value);
				break;
			case UUIDData:
					eval((UUID) value);
				break;
			default:
				throw new PieException(String.format("Aggregatio operations do not currently support %s data types.", _dataType.toString()));
			}
		}
	}

	public Object getResult() {
		calculate();

		if (_objValue != null) {
			return _objValue;
		}

		switch (_dataType) {
		case StringData:
			return _stringValue;
		case BigDecimalData:
			return _bigdecimalValue;
		case ByteData:
			return _byteValue;
		case DoubleData:
			return _doubleValue;
		case FloatData:
			return _floatValue;
		case IntegerData:
			return _intValue;
		case LongData:
			return _longValue;
		case ShortData:
			return _shortValue;
		case CharData:
			return _charValue;
		case DateData:
			return _dateValue;
		case BooleanData:
			return _booleanValue;
		case UUIDData:
			return _uuidValue;
		default:
			throw new PieException(String.format("Aggregatio operations do not currently support %s data types.", _dataType.toString()));
		}
	}
	
	public String getNewColumnName() {
		return _newColumnName;
	}
	
	public void setNewColumnName(String value) {
		_newColumnName = value;
	}
	
	public DataType getNewColumnType() {
		return _newColumnType;
	}
	
	public void setDataColumnIndex(int value) {
		_dataColumnIndex = value;
	}
	
	public int getDataColumnIndex() {
		return _dataColumnIndex;
	}
	
	public void setGroupValues(Object[] values) {
		if (values != null) {
			_groupValues = new Object[values.length];
			for(int i= 0;i<values.length;i++) {
				_groupValues[i] = values[i];
			}
		}
	}
	
	public String getGroupValuesAsCSV() {
		if (_groupValues == null) {
			return "- No values defined -";
		}
		
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<_groupValues.length;i++) {
			if (i > 0) {
				sb.append(", ");
			}
			if (_groupValues[i] == null) {
				sb.append("null");
			} else {
			sb.append(_groupValues[i].toString());
			}
		}
		return sb.toString();
	}
	
	public abstract Aggregation clone();

	protected abstract DataType newColumnType();
	
	protected abstract void calculate();

	protected abstract void eval(String value);

	protected abstract void eval(BigDecimal value);

	protected abstract void eval(byte value);

	protected abstract void eval(double value);

	protected abstract void eval(float value);

	protected abstract void eval(int value);

	protected abstract void eval(long value);

	protected abstract void eval(short value);

	protected abstract void eval(Date value);

	protected abstract void eval(boolean value);

	protected abstract void eval(UUID value);

	protected abstract void eval(char value);

}