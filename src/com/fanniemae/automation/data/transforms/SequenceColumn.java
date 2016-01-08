package com.fanniemae.automation.data.transforms;

import org.w3c.dom.Element;

import com.fanniemae.automation.SessionManager;
import com.fanniemae.automation.common.StringUtilities;

public class SequenceColumn extends DataTransform {
	protected int _SequenceNumber = 1;
	protected int _Increment = 1;

	/**
	 * 
	 * @author Richard Monson
	 * @since 2016-01-07
	 * 
	 */
	public SequenceColumn(SessionManager session, Element operation) {
		super(session, operation);

		String sStartNumber = _Transform.getAttribute("StartNumber");
		int iStartNumber = StringUtilities.toInteger(sStartNumber);
		if (StringUtilities.isNotNullOrEmpty(sStartNumber) && (iStartNumber == Integer.MIN_VALUE)) {
			throw new RuntimeException("The StartNumber attribute of a SequenceColumn must be a valid integer.");
		} else if (StringUtilities.isNotNullOrEmpty(sStartNumber)) {
			_SequenceNumber = iStartNumber;
		}

		String sIncrement = _Transform.getAttribute("Increment");
		int iIncrement = StringUtilities.toInteger(sIncrement);
		if (StringUtilities.isNotNullOrEmpty(sIncrement) && (iIncrement == Integer.MIN_VALUE)) {
			throw new RuntimeException("The Increment attribute of a SequenceColumn must be a valid integer.");
		} else if (StringUtilities.isNotNullOrEmpty(sIncrement)) {
			_Increment = iIncrement;
		}
	}

	@Override
	public Object[] processDataRow(Object[] dataRow) {
		if (dataRow == null) {
			return dataRow;
		}

		dataRow[_OutColumnIndex] = _SequenceNumber;
		_SequenceNumber += _Increment;
		_RowsProcessed++;
		return dataRow;
	}

	@Override
	public boolean isTableLevel() {
		return false;
	}

}
