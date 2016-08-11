package com.fanniemae.devtools.pie.data.transforms;

import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.StringUtilities;

/**
 * 
 * @author Richard Monson
 * @since 2016-01-20
 * 
 */
public class SequenceColumn extends DataTransform {
	protected int _sequenceNumber = 1;
	protected int _increment = 1;

	/**
	 * 
	 * @author Richard Monson
	 * @since 2016-01-07
	 * 
	 */
	public SequenceColumn(SessionManager session, Element transform) {
		super(session, transform);
		_columnType = "java.lang.Integer";
		
		String sStartNumber = _transform.getAttribute("StartNumber");
		int iStartNumber = StringUtilities.toInteger(sStartNumber);
		if (StringUtilities.isNotNullOrEmpty(sStartNumber) && (iStartNumber == Integer.MIN_VALUE)) {
			throw new RuntimeException("The StartNumber attribute of a SequenceColumn must be a valid integer.");
		} else if (StringUtilities.isNotNullOrEmpty(sStartNumber)) {
			_sequenceNumber = iStartNumber;
		}
		_transformInfo.appendFormatLine("StartNumber = %d", _sequenceNumber);

		String sIncrement = _transform.getAttribute("Increment");
		int iIncrement = StringUtilities.toInteger(sIncrement);
		if (StringUtilities.isNotNullOrEmpty(sIncrement) && (iIncrement == Integer.MIN_VALUE)) {
			throw new RuntimeException("The Increment attribute of a SequenceColumn must be a valid integer.");
		} else if (StringUtilities.isNotNullOrEmpty(sIncrement)) {
			_increment = iIncrement;
		}
		_transformInfo.appendFormat("Increment = %d", _increment);
		//addTransformLogMessage();
	}

	@Override
	public Object[] processDataRow(Object[] dataRow) {
		if (dataRow == null) {
			return dataRow;
		}
		dataRow = addDataColumn(dataRow);
		dataRow[_outColumnIndex] = _sequenceNumber;
		_sequenceNumber += _increment;
		_rowsProcessed++;
		return dataRow;		
	}

	@Override
	public boolean isTableLevel() {
		return false;
	}

}
