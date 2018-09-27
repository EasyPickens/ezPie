/**
 *  
 * Copyright (c) 2017 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 * 
**/

package com.fanniemae.ezpie.common;

/**
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2017-11-21
 * 
 */

public class ExceptionUtilities {
	
	private ExceptionUtilities() {
	}
	
	public static void goSilent(Exception ex) {
		return;
	}
	
	public static String getMessage(Exception ex) {
		return ex.getMessage();
	}

	public static String getStackTrace(Exception ex) {
		StringBuilder sbStack = new StringBuilder(ex.getMessage());
		for (StackTraceElement ele : ex.getStackTrace()) {
			sbStack.append(System.getProperty("line.separator"));
			sbStack.append(ele.toString());
		}
		return sbStack.toString();
	}	

}
