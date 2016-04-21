package com.fanniemae.devtools.pie.data.transforms.compare;

import java.math.BigDecimal;

public class CompareBigDecimal extends Compare {

	protected BigDecimal _compareValue;

	public CompareBigDecimal(Object compareValue) {
		if (compareValue != null) {
			_compareValue = (BigDecimal) compareValue;
		}
	}

	@Override
	public int compareTo(Object value) {

		if ((_compareValue == null) && (value == null)) {
			return 0;
		} else if (value == null) {
			return 1;
		} else if (_compareValue == null) {
			return -1;
		}
		return _compareValue.compareTo((BigDecimal) value);
	}
}
