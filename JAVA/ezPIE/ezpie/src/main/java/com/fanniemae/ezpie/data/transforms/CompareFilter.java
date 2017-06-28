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
 * @since 2016-03-02
 * 
*/

public class CompareFilter extends DataTransform {

	protected enum CompareType {
		EQUALS, NOT_EQUALS, LESS_THAN, GREATER_THAN, LESS_THAN_EQUAL_TO, GREATER_THAN_EQUAL_TO
	}

	protected CompareType _compareType = CompareType.EQUALS;

	protected String _compareValue;

	public CompareFilter(SessionManager session, Element transform, boolean idRequired) {
		super(session, transform, false);

		_dataColumn = _session.getAttribute(transform, "DataColumn");
		_compareType = setCompareType(_session.getAttribute(transform, "Operation"));
		_compareValue = _session.getAttribute(transform, "CompareValue");

		if (StringUtilities.isNullOrEmpty(_dataColumn)) {
			throw new RuntimeException(String.format("%s transform requires a column name in DataColumn.", transform.getNodeName()));
		}

	}

	@Override
	public Object[] processDataRow(Object[] dataRow) {
		return null;
	}

	protected CompareType setCompareType(String value) {
		if (StringUtilities.isNullOrEmpty(value)) {
			return CompareType.EQUALS;
		}
		switch (value.toLowerCase().replace(" ", "")) {
		case "equal":
		case "equals":
		case "==":
		case "=":
			return CompareType.EQUALS;
		case "notequal":
		case "notequals":
		case "!=":
		case "<>":
			return CompareType.NOT_EQUALS;
		case "lessthan":
		case "<":
			return CompareType.LESS_THAN;
		case "greaterthan":
		case ">":
			return CompareType.GREATER_THAN;
		case "lessthanequal":
		case "lessthanequalto":
		case "<=":
			return CompareType.LESS_THAN_EQUAL_TO;
		case "greaterthanequal":
		case "greaterthanequalto":
		case ">=":
			return CompareType.GREATER_THAN_EQUAL_TO;
		default:
			throw new RuntimeException(String.format("%s is not a supported compare type.  Please use <, >, =, <=, or >=.", value));
		}
	}
}
