package com.fanniemae.ezpie.data.transforms.aggregations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2018-10-20
 * 
 */

public class Median extends Aggregation {

	List<String> _strings = new ArrayList<String>();
	List<Integer> _integers = new ArrayList<Integer>();
	List<Long> _longs = new ArrayList<Long>();
	List<Double> _doubles = new ArrayList<Double>();
	List<Boolean> _booleans = new ArrayList<Boolean>();
	List<Byte> _bytes = new ArrayList<Byte>();
	List<Character> _chars = new ArrayList<Character>();
	List<Date> _dates = new ArrayList<Date>();
	List<UUID> _uuids = new ArrayList<UUID>();
	List<BigDecimal> _bigdecimals = new ArrayList<BigDecimal>();

	public Median(DataType columnDataType, int dataColumnIndex) {
		super(columnDataType, dataColumnIndex);
	}

	@Override
	public Aggregation clone() {
		Aggregation agg = new Median(_dataType, _dataColumnIndex);
		agg.setNewColumnName(_newColumnName);
		return agg;
	}

	@Override
	protected void calculate() {
		switch (_dataType) {
		case StringData:
			findMedian(_strings);
			break;
		case IntegerData:
		case ShortData:
			findMedian(_integers);
			break;
		case LongData:
			findMedian(_longs);
			break;
		case DoubleData:
		case FloatData:
			findMedian(_doubles);
			break;
		case BooleanData:
			findMedian(_booleans);
			break;
		case ByteData:
			findMedian(_bytes);
			break;
		case CharData:
			findMedian(_chars);
			break;
		case DateData:
			findMedian(_dates);
			break;
		case UUIDData:
			findMedian(_uuids);
			break;
		case BigDecimalData:
			findMedian(_bigdecimals);
			break;
		default:
			throw new PieException("Median operation is not currently supported on this data type.");
		}

	}

	@Override
	protected void eval(String value) {
		if (value != null) {
			_strings.add(value);
		}
	}

	@Override
	protected void eval(BigDecimal value) {
		if (value != null) {
			_bigdecimals.add(value);
		}
	}

	@Override
	protected void eval(byte value) {
		_bytes.add(value);
	}

	@Override
	protected void eval(double value) {
		_doubles.add(value);
	}

	@Override
	protected void eval(float value) {
		_doubles.add((double) value);
	}

	@Override
	protected void eval(int value) {
		_integers.add(value);
	}

	@Override
	protected void eval(long value) {
		_longs.add(value);
	}

	@Override
	protected void eval(short value) {
		_integers.add((int) value);
	}

	@Override
	protected void eval(Date value) {
		if (value != null) {
			_dates.add(value);
		}
	}

	@Override
	protected void eval(boolean value) {
		_booleans.add(value);
	}

	@Override
	protected void eval(UUID value) {
		if (value != null) {
			_uuids.add(value);
		}
	}

	@Override
	protected void eval(char value) {
		_chars.add(value);

	}

	@Override
	protected DataType newColumnType() {
		if (_dataType == DataType.FloatData) {
			return DataType.DoubleData;
		} else if (_dataType == DataType.ShortData) {
			return DataType.IntegerData;
		} else {
			return _dataType;
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected <T extends Comparable> void findMedian(List<T> collection) {
		if ((collection == null) || (collection.size() == 0)) {
			_objValue = null;
			return;
		}

		Collections.sort(collection);
		int count = collection.size();
		int first = -1;
		int last = -1;
		if (count == 1) {
			_objValue = collection.get(0);
		} else if (count % 2 == 0) {
			// list contains an even number of items, need to average the two nearest the middle
			first = (count / 2) - 1;
			last = first + 1;
			Object value1 = collection.get(first);
			Object value2 = collection.get(last);
			if (collection.get(0) instanceof Integer) {
				_objValue = ((int) value1 + (int) value2) / 2D;
			} else if (collection.get(0) instanceof Long) {
				_objValue = ((long) value1 + (long) value2) / 2D;
			} else if (collection.get(0) instanceof Double) {
				_objValue = ((double) value1 + (double) value2) / 2D;
			} else if (collection.get(0) instanceof BigDecimal) {
				BigDecimal median = ((BigDecimal) value1).add((BigDecimal) value2);
				_objValue = median.divide(new BigDecimal(2));
			} else if (collection.get(0) instanceof Date) {
				Date d1 = (Date) value1;
				Date d2 = (Date) value2;
				long seconds = (d2.getTime() - d1.getTime()) / 1000;
				long middle = seconds / 2;
				Calendar calDate = Calendar.getInstance();
				calDate.setTime(d1);
				calDate.add(Calendar.SECOND, (int) middle);
				_objValue = calDate.getTime();
			} else {
				_objValue = value1;
			}
		} else {
			_objValue = collection.get((int) Math.ceil(count / 2D) - 1);
		}
	}

}
