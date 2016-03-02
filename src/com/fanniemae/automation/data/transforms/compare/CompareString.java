package com.fanniemae.automation.data.transforms.compare;

public class CompareString extends Compare {

	protected String _compareValue;

	public CompareString(Object compareValue) {
		if (compareValue != null) {
			_compareValue = (String) compareValue;
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
		return _compareValue.compareTo((String) value);
	}
	
}
