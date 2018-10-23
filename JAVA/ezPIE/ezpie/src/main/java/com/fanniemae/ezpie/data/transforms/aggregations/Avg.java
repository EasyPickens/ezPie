package com.fanniemae.ezpie.data.transforms.aggregations;

import java.math.BigDecimal;

import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2018-10-20
 * 
 */

public class Avg extends Sum {

	public Avg(DataType columnDataType, int dataColumnIndex) {
		super(columnDataType, dataColumnIndex);
	}
	
	@Override
	public Aggregation clone() {
		Aggregation agg = new Avg(_dataType, _dataColumnIndex);
		agg.setNewColumnName(_newColumnName);
		return agg;
	}

	@Override
	protected void calculate() {
		// If the type changes, then add the original to the sum (to get the first value)
		switch (_dataType) {
		case ByteData:
			_objValue = (_intValue + _byteValue) / _totalCount;
			break;
		case IntegerData:
			_objValue = (_longValue + _intValue) / _totalCount;
			break;
		case ShortData:
			_objValue = (_intValue + _shortValue) / _totalCount;
			break;
		case BigDecimalData:
			_objValue = _bigdecimalValue.divide(new BigDecimal(_totalCount));
			break;
		case DoubleData:
			_objValue = _doubleValue / _totalCount;
			break;
		case FloatData:
			_objValue = _floatValue / _totalCount;
			break;
		case LongData:
			_objValue = _longValue / _totalCount;
			break;
		default:
			throw new PieException(String.format("Average does not support %s data type.", _dataType.toString()));
		}
	}

}
