package com.fanniemae.ezpie.data.transforms.aggregations;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2018-10-20
 * 
 */

public class Last extends Aggregation {

	public Last(DataType columnDataType, int dataColumnIndex) {
		super(columnDataType, dataColumnIndex);
	}
	
	@Override
	public Aggregation clone() {
		Aggregation agg = new Last(_dataType, _dataColumnIndex);
		agg.setNewColumnName(_newColumnName);
		return agg;
	}

	@Override
	protected void calculate() {
		// Nothing to calculate.
	}

	@Override
	protected void eval(String value) {
		_stringValue = value;
	}

	@Override
	protected void eval(BigDecimal value) {
		_bigdecimalValue = value;
	}

	@Override
	protected void eval(byte value) {
		_byteValue = value;
	}

	@Override
	protected void eval(double value) {
		_doubleValue = value;
	}

	@Override
	protected void eval(float value) {
		_floatValue = value;
	}

	@Override
	protected void eval(int value) {
		_intValue = value;
	}

	@Override
	protected void eval(long value) {
		_longValue = value;
	}

	@Override
	protected void eval(short value) {
		_shortValue = value;
	}

	@Override
	protected void eval(Date value) {
		_dateValue = value;
	}

	@Override
	protected void eval(boolean value) {
		_booleanValue = value;
	}

	@Override
	protected void eval(UUID value) {
		_uuidValue = value;
	}

	@Override
	protected void eval(char value) {
		_charValue = value;
	}
	
	@Override
	protected DataType newColumnType() {
		return _dataType;
	}

}
