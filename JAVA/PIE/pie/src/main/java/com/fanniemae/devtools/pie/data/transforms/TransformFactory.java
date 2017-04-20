/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPIE/wiki/Fannie-Mae-Open-Source-Licensing-Project
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.devtools.pie.data.transforms;

import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.SessionManager;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-02-09
 * 
*/

public class TransformFactory {

	public static DataTransform getTransform(SessionManager session, Element transform) {
		switch (transform.getNodeName()) {
		case "TimespanColumn":
			return new TimespanColumn(session, transform);
		case "SequenceColumn":
			return new SequenceColumn(session, transform);
		case "Sort":
			return new Sort(session, transform);
		case "Join":
			return new Join(session, transform);
		case "Column":
			return null;
		default:
			throw new RuntimeException(String.format("%s data transformation not currently supported.", transform.getNodeName()));
		}
	}

	public static DataTransform getIndexTransform(SessionManager session, String[] indexColumns) {
		return new Index(session, indexColumns);
	}

}
