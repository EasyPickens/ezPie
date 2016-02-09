package com.fanniemae.automation.data.transforms;

import org.w3c.dom.Element;

import com.fanniemae.automation.SessionManager;

public class TransformFactory {
	
	public static DataTransform getTransform(SessionManager session, Element transform) {
		switch (transform.getNodeName()) {
		case "TimespanColumn":
			return new TimespanColumn(session, transform);
		case "SequenceColumn":
			return new SequenceColumn(session, transform);
		case "Sort":
			return new Sort(session, transform);
		default:
			throw new RuntimeException(String.format("%s data transformation not currently supported.", transform.getNodeName()));
		}
	}
	
	public static DataTransform getIndexTransform(SessionManager session, String[] indexColumns) {
		return new Index(session, indexColumns);
	}

}
