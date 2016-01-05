package com.fanniemae.automation.common;

public class ReportBuilder {

	protected StringBuilder _sb = new StringBuilder();;
	
	public void append(String value) {
		_sb.append(value);
	}
	
	public void appendLine(String value) {
		append(value);
		_sb.append(System.lineSeparator());
	}

	public void appendFormat(String value, Object... arguments) {
		_sb.append(String.format(value, arguments));
	}
	
	public void appendFormatLine(String value, Object... arguments) {
		appendFormat(value, arguments);
		_sb.append(System.lineSeparator());
	}
	
	public String toString() {
		return _sb.toString();
	}
}
