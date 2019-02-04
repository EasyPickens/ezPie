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

public class Count extends Aggregation {

	public Count(DataType columnDataType, int dataColumnIndex) {
		super(columnDataType, dataColumnIndex);
	}
	
	@Override
	public Aggregation clone() {
		Aggregation agg = new Count(_dataType, _dataColumnIndex);
		agg.setNewColumnName(_newColumnName);
		return agg;
	}

	@Override
	protected void calculate() {
		_objValue = _countWithoutNulls;
	}

	@Override
	protected void eval(String value) {
	}

	@Override
	protected void eval(BigDecimal value) {
	}

	@Override
	protected void eval(byte value) {
	}

	@Override
	protected void eval(double value) {
	}

	@Override
	protected void eval(float value) {
	}

	@Override
	protected void eval(int value) {
	}

	@Override
	protected void eval(long value) {
	}

	@Override
	protected void eval(short value) {
	}

	@Override
	protected void eval(Date value) {
	}

	@Override
	protected void eval(boolean value) {
	}

	@Override
	protected void eval(UUID value) {
	}

	@Override
	protected void eval(char value) {
	}

	@Override
	protected DataType newColumnType() {
		return DataType.IntegerData;
	}
}