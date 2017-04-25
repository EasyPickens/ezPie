/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.data.transforms;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.StringUtilities;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-01-07
 * 
*/

public class SequenceColumn extends DataTransform {
	protected int _sequenceNumber = 1;
	protected int _increment = 1;

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
