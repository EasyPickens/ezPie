package com.fanniemae.automation.data.transforms.compare;

import java.sql.Timestamp;

public class CompareSqlTimestamp extends Compare {

	protected Timestamp _compareValue;

	public CompareSqlTimestamp(Object compareValue) {
		if (compareValue != null) {
			_compareValue = (Timestamp) compareValue;
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
		return _compareValue.compareTo((Timestamp) value);
	}
	
}
