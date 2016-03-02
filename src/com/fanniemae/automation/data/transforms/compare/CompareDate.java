package com.fanniemae.automation.data.transforms.compare;

import java.util.Date;

public class CompareDate extends Compare {

	protected Date _compareValue;

	public CompareDate(Object compareValue) {
		if (compareValue != null) {
			_compareValue = (Date) compareValue;
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
		return _compareValue.compareTo((Date) value);
	}

}
