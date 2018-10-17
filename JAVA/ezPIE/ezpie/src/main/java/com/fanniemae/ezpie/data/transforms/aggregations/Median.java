package com.fanniemae.ezpie.data.transforms.aggregations;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

public class Median extends Aggregation {

	public Median(DataType columnDataType) {
		super(columnDataType);
		throw new PieException("Median has not been implemented yet.");
	}

	@Override
	protected void calculate() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void eval(String value) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void eval(BigDecimal value) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void eval(byte value) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void eval(double value) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void eval(float value) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void eval(int value) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void eval(long value) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void eval(short value) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void eval(Date value) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void eval(boolean value) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void eval(UUID value) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void eval(char value) {
		// TODO Auto-generated method stub

	}

}
