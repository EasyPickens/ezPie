package com.fanniemae.devtools.pie.data.transforms.compare;

public class CompareInteger extends Compare {

	protected Integer _compareValue;

	public CompareInteger(Object compareValue) {
		if (compareValue != null) {
			_compareValue = (int) compareValue;
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
		return _compareValue.compareTo((int) value);
	}


}
