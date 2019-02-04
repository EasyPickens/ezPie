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

public class Max extends Aggregation {

	public Max(DataType columnDataType, int dataColumnIndex) {
		super(columnDataType, dataColumnIndex);
	}
	
	@Override
	public Aggregation clone() {
		Aggregation agg = new Max(_dataType, _dataColumnIndex);
		agg.setNewColumnName(_newColumnName);
		return agg;
	}

	@Override
	protected void calculate() {
		// No calculations required.
	}

	@Override
	protected void eval(String value) {
		if (_stringValue.compareTo(value) == 1) {
			_stringValue = value;
		}
	}

	@Override
	protected void eval(BigDecimal value) {
		if (_bigdecimalValue.compareTo(value) == 1) {
			_bigdecimalValue = value;
		}

	}

	@Override
	protected void eval(byte value) {
		if (_byteValue < value) {
			_byteValue = value;
		}
	}

	@Override
	protected void eval(double value) {
		if (_doubleValue < value) {
			_doubleValue = value;
		}
	}

	@Override
	protected void eval(float value) {
		if (_floatValue < value) {
			_floatValue = value;
		}
	}

	@Override
	protected void eval(int value) {
		if (_intValue < value) {
			_intValue = value;
		}
	}

	@Override
	protected void eval(long value) {
		if (_longValue < value) {
			_longValue = value;
		}
	}

	@Override
	protected void eval(short value) {
		if (_shortValue < value) {
			_shortValue = value;
		}
	}

	@Override
	protected void eval(Date value) {
		if (_dateValue.before(value)) {
			_dateValue = value;
		}
	}

	@Override
	protected void eval(boolean value) {
		throw new PieException("Max aggregation cannot be applied to boolean values.");
	}

	@Override
	protected void eval(UUID value) {
		if (_uuidValue.compareTo(value) == 1) {
			_uuidValue = value;
		}
	}

	@Override
	protected void eval(char value) {
		if (_charValue < value) {
			_charValue = value;
		}
	}

	@Override
	protected DataType newColumnType() {
		return _dataType;
	}
}
