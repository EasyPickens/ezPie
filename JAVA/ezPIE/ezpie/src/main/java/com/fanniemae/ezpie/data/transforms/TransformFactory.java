/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.data.transforms;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.PieException;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-02-09
 * 
*/

public class TransformFactory {
	
	private TransformFactory() {}

	public static DataTransform getTransform(SessionManager session, Element transform) {
		switch (transform.getNodeName()) {
		case "Timespan":
		case "TimespanColumn":
			return new TimespanColumn(session, transform);
		case "Sequence":
		case "SequenceColumn":
			return new SequenceColumn(session, transform);
		case "Calculation":
			return new CalculationColumn(session, transform);
		case "Color":
			return new ColorColumn(session, transform);
		case "Sort":
			return new Sort(session, transform);
		case "Join":
			return new Join(session, transform);
		case "ColumnFilter":
			return new ColumnFilter(session, transform);
		case "SqlParameter":
			return null;
		case "Rename":
		case "RenameColumn":
			return new RenameColumn(session, transform);
		case "Column":
			return null;
		default:
			throw new PieException(String.format("%s data transformation is not currently supported.", transform.getNodeName()));
		}
	}

	public static DataTransform getIndexTransform(SessionManager session, String[] indexColumns) {
		return new Index(session, indexColumns);
	}

}
