package com.fanniemae.automation.data.transforms;

import org.w3c.dom.Element;

import com.fanniemae.automation.SessionManager;
import com.fanniemae.automation.common.StringUtilities;

public class CompareFilter extends DataTransform {

	protected enum CompareType {
		EQUALS, NOT_EQUALS, LESS_THAN, GREATER_THAN, LESS_THAN_EQUAL_TO, GREATER_THAN_EQUAL_TO
	}

	protected CompareType _compareType = CompareType.EQUALS;

	protected String _compareValue;

	public CompareFilter(SessionManager session, Element transform, boolean idRequired) {
		super(session, transform, false);

		_DataColumn = _Session.getAttribute(transform, "DataColumn");
		_compareType = setCompareType(_Session.getAttribute(transform, "Operation"));
		_compareValue = _Session.getAttribute(transform, "CompareValue");

		if (StringUtilities.isNullOrEmpty(_DataColumn)) {
			throw new RuntimeException(String.format("%s transform requires a column name in DataColumn.", transform.getNodeName()));
		}

	}

	@Override
	public boolean isTableLevel() {
		return false;
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
