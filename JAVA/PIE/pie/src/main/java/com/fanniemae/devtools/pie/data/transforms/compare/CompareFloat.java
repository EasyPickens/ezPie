package com.fanniemae.devtools.pie.data.transforms.compare;

public class CompareFloat extends Compare {

	protected Float _compareValue;

	public CompareFloat(Object compareValue) {
		if (compareValue != null) {
			_compareValue = (float) compareValue;
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

		return _compareValue.compareTo((float) value);
	}
}
