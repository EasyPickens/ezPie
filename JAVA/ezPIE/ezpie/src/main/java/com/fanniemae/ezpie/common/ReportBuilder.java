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

package com.fanniemae.ezpie.common;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-05-30
 * 
 */

public class ReportBuilder {

	protected StringBuilder _sb = new StringBuilder();;
	
	public boolean hasText() {
		return _sb.length() > 0;
	}
	
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
	
	public void appendArray(String[] lines) {
		for(int i=0;i<lines.length; i++) {
			_sb.append(lines[i]);
			_sb.append(System.lineSeparator());
		}
	}
	
	@Override
	public String toString() {
		return _sb.toString();
	}
}
