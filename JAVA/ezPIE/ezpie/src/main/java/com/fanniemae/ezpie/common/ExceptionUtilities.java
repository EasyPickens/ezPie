package com.fanniemae.ezpie.common;

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
