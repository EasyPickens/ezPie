package com.fanniemae.devtools.pie.data.transforms.compare;

public class CompareDouble extends Compare {

	protected Double _compareValue;

	public CompareDouble(Object compareValue) {
		if (compareValue != null) {
			_compareValue = (double) compareValue;
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

		return _compareValue.compareTo((double) value);
	}

}
