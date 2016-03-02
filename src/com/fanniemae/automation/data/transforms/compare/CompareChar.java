package com.fanniemae.automation.data.transforms.compare;

public class CompareChar extends Compare {

	protected Character _compareValue;

	public CompareChar(Object compareValue) {
		if (compareValue != null) {
			_compareValue = (char) compareValue;
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
		return _compareValue.compareTo((char) value);
	}

}
