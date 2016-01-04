package com.fanniemae.automation.common;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-15
 * 
 */
public class StringUtilities {

	public static boolean isNotNullOrEmpty(String value) {
		return !isNullOrEmpty(value);
	}

	public static boolean isNullOrEmpty(String value) {
		return (value == null) || value.isEmpty();
	}

	public static boolean isFormattedDate(String value) {
		// Check for ISO 8601 Date Format (yyyy-MM-ddTHH:mm:ss)
		if (value.length() > 14) {
			String sCheck = value.substring(4, 1) + value.substring(7, 1) + value.substring(10, 1) + value.substring(13, 1);
			return sCheck.equals("--T:");
		}
		return false;
	}

	public static boolean isFormula(String value) {
		char[] aSymbols = ".*/+-()=<>!^#&@$%\\|{}'?".toCharArray();
		for (int i = 0; i < aSymbols.length; i++) {
			if (value.indexOf(aSymbols[i]) != -1) {
				return true;
			}
		}
		return false;
	}

	public static Boolean toBoolean(String value) {
		return toBoolean(value, false);
	}

	public static Boolean toBoolean(String value, Boolean defaultValue) {
		if (isNullOrEmpty(value))
			return defaultValue;
		else if ("t|y|1".indexOf(value.toLowerCase().substring(1, 1)) > -1)
			return true;
		else if (value.toCharArray().equals("on"))
			return true;
		else
			return false;
	}

	public static int toInteger(String value) {
		return toInteger(value, 0);
	}

	public static int toInteger(String value, int defaultValue) {
		if (isNullOrEmpty(value))
			return defaultValue;
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}

	public static long toLong(String value) {
		return toLong(value, 0L);
	}

	public static long toLong(String value, long defaultValue) {
		if (isNullOrEmpty(value))
			return defaultValue;
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}

	public static double toDouble(String value) {
		return toDouble(value, 0.0);
	}

	public static double toDouble(String value, double defaultValue) {
		if (isNullOrEmpty(value))
			return defaultValue;
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}

	public static BigDecimal toBigDecimal(String value) {
		return toBigDecimal(value, new BigDecimal("0.0"));
	}

	public static BigDecimal toBigDecimal(String value, BigDecimal defaultValue) {
		if (isNullOrEmpty(value))
			return defaultValue;
		try {
			return new BigDecimal(value);
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}

	public static Object toObject(String typeName, String value) {
		if ((value == null) || value.isEmpty()) {
			return "";
		}

		switch (typeName) {
		case "Boolean":
			return Boolean.parseBoolean(value);
		case "Byte":
		case "SByte":
			return Byte.parseByte(value);
		case "Byte[]":
			return value.toCharArray();
		case "Char":
			return value.charAt(0);
		case "Char[]":
			return value.toCharArray();
		case "DateTime":
			Date dtValue = new Date(Long.parseLong(value));
			if (dtValue == new Date(Long.MIN_VALUE)) {
				return null;
			}
			return dtValue;
		case "Decimal":
		case "Double":
		case "Single":
			return Double.parseDouble(value);
		case "Float":
			return Float.parseFloat(value);
		case "UUID":
			return UUID.fromString(value);
		case "Int":
		case "Integer":
		case "Int16":
		case "Int32":
		case "UInt16":
			return Integer.parseInt(value);
		case "Int64":
		case "Long":
		case "UInt32":
		case "UInt64":
			return Long.parseLong(value);
		case "NCLOB":
			return "NCLOB";
		case "Short":
			return Short.parseShort(value);
		case "String":
			return value;
		case "TimeSpan":
			return value;
		default:
			return null;
		}
	}

	public static String wrapValue(String value) {
		if ((value == null) || (value.indexOf(' ') == -1))
			return value;
		return String.format("\"%s\"", value);
	}
}
