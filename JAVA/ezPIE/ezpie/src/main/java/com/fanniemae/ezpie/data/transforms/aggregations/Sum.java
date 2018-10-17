package com.fanniemae.ezpie.data.transforms.aggregations;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

public class Sum extends Aggregation {

	public Sum(DataType columnDataType) {
		super(columnDataType);
	}

	@Override
	protected void calculate() {
		// If the type changes, then add the original to the sum (to get the first value).
		switch (_dataType) {
		case ByteData:
			_objValue = _intValue + _byteValue;
			break;
		case IntegerData:
			_objValue = _longValue + _intValue;
			break;
		case ShortData:
			_objValue = _intValue + _shortValue;
			break;
		default:
			// Sum type matches the original column data type.
		}

	}

	@Override
	protected void eval(String value) {
		throw new PieException("Sum aggregation cannot be applied to string values.");
	}

	@Override
	protected void eval(BigDecimal value) {
		_bigdecimalValue.add(value);
	}

	@Override
	protected void eval(byte value) {
		_intValue += value;
	}

	@Override
	protected void eval(double value) {
		_doubleValue += value;
	}

	@Override
	protected void eval(float value) {
		_floatValue += value;
	}

	@Override
	protected void eval(int value) {
		_longValue += value;
	}

	@Override
	protected void eval(long value) {
		_longValue += value;
	}

	@Override
	protected void eval(short value) {
		_intValue += value;
	}

	@Override
	protected void eval(Date value) {
		throw new PieException("Sum aggregation cannot be applied to date values.");
	}

	@Override
	protected void eval(boolean value) {
		throw new PieException("Sum aggregation cannot be applied to boolean values.");
	}

	@Override
	protected void eval(UUID value) {
		throw new PieException("Sum aggregation cannot be applied to UUID values.");
	}

	@Override
	protected void eval(char value) {
		throw new PieException("sum aggregation cannot be applied to char values.");
	}

}
