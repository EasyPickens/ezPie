package com.fanniemae.devtools.pie.data.transforms.compare;

import java.util.UUID;

public class CompareUUID extends Compare {

	protected UUID _compareValue;

	public CompareUUID(Object compareValue) {
		if (compareValue != null) {
			_compareValue = (UUID) compareValue;
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
		return _compareValue.compareTo((UUID) value);
	}
	
}
