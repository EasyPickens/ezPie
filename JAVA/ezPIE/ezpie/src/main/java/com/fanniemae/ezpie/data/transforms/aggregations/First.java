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

public class First extends Aggregation {

	/*
	 * The parent class will automatically take the first entry in the dataset. So this class doesn't need to perform any operations.
	 */

	public First(DataType columnDataType, int dataColumnIndex) {
		super(columnDataType, dataColumnIndex);
	}

	@Override
	public Aggregation clone() {
		Aggregation agg = new First(_dataType, _dataColumnIndex);
		agg.setNewColumnName(_newColumnName);
		return agg;
	}

	@Override
	protected void calculate() {
	}

	@Override
	protected void eval(String value) {
		assignFirstValue(value);
	}

	@Override
	protected void eval(BigDecimal value) {
		assignFirstValue(value);
	}

	@Override
	protected void eval(byte value) {
		assignFirstValue(value);
	}

	@Override
	protected void eval(double value) {
		assignFirstValue(value);
	}

	@Override
	protected void eval(float value) {
		assignFirstValue(value);
	}

	@Override
	protected void eval(int value) {
		assignFirstValue(value);
	}

	@Override
	protected void eval(long value) {
		assignFirstValue(value);
	}

	@Override
	protected void eval(short value) {
		assignFirstValue(value);
	}

	@Override
	protected void eval(Date value) {
		assignFirstValue(value);
	}

	@Override
	protected void eval(boolean value) {
		assignFirstValue(value);
	}

	@Override
	protected void eval(UUID value) {
		assignFirstValue(value);
	}

	@Override
	protected void eval(char value) {
		assignFirstValue(value);
	}

	@Override
	protected DataType newColumnType() {
		return _dataType;
	}
	
	protected void assignFirstValue(Object value) {
		if (_isFirst) {
			_objValue = value;
			_isFirst = false;
		}
	}

}
