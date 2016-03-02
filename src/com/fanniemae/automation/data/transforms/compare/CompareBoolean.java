package com.fanniemae.automation.data.transforms.compare;

public class CompareBoolean extends Compare {

	protected Boolean _compareValue;

	public CompareBoolean(Object compareValue) {
		if (compareValue != null) {
			_compareValue = (boolean) compareValue;
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
		return _compareValue.compareTo((boolean) value);
	}

}
