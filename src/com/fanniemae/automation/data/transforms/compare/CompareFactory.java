package com.fanniemae.automation.data.transforms.compare;

import com.fanniemae.automation.datafiles.lowlevel.DataFileEnums.DataType;

public class CompareFactory {
	
	public static Compare getCompareMethod(DataType dataType, Object compareValue) {
		switch (dataType) {
		case BigDecimalData:
			return new CompareBigDecimal(compareValue);
		case BooleanData:
			return new CompareBoolean(compareValue);
		case ByteData:
			return new CompareByte(compareValue);
		case CharData:
			return new CompareChar(compareValue);
		case DateData:
			return new CompareDate(compareValue);
		case DoubleData:
			return new CompareDouble(compareValue);
		case FloatData:
			return new CompareFloat(compareValue);
		case IntegerData:
			return new CompareInteger(compareValue);
		case LongData:
			return new CompareLong(compareValue);
		case ShortData:
			return new CompareShort(compareValue);
		case SqlTimestampData:
			return new CompareSqlTimestamp(compareValue);
		case StringData:
			return new CompareString(compareValue);
		case UUIDData:
			return new CompareUUID(compareValue);
		default:
			throw new RuntimeException(String.format("No compare class defined for data type %s.", dataType.name()));
		}
	}
	
}
