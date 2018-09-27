/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.data.transforms.compare;

import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2016-03-02
 * 
 **/

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
			throw new PieException(String.format("No compare class defined for data type %s.", dataType.name()));
		}
	}
	
}
