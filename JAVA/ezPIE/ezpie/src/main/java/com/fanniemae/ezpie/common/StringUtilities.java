/**
 *  
 * Copyright (c) 2015 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.common;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.lang3.time.DateUtils;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2015-12-15
 * 
 */
public final class StringUtilities {

	// @formatter:off
	// Source: http://docs.oracle.com/javase/6/docs/api/java/lang/Double.html#valueOf%28java.lang.String%29
	protected static String DOUBLE_REGEX = "[\\x00-\\x20]*[+-]?(NaN|Infinity|((((\\p{Digit}+)(\\.)?((\\p{Digit}+)?)([eE][+-]?(\\p{Digit}+))?)|(\\.((\\p{Digit}+))([eE][+-]?(\\p{Digit}+))?)|(((0[xX](\\p{XDigit}+)(\\.)?)|(0[xX](\\p{XDigit}+)?(\\.)(\\p{XDigit}+)))[pP][+-]?(\\p{Digit}+)))[fFdD]?))[\\x00-\\x20]*";
	
	protected static String BOOLEAN_VALUES = "|true|false|t|f|0|1|yes|no|on|off|y|n|";
	
	protected static String[] SUPPORTED_DATE_FORMATS = new String[] { "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss", "yyyy-MM-dd HH:mm:ss", 
                                                            "yyyy/MM/dd HH:mm:ss", "MM-dd-yyyy HH:mm:ss", "MM/dd/yyyy HH:mm:ss", "dd-MM-yyyy HH:mm:ss",
                                                            "dd/MM/yyyy HH:mm:ss", "MM-yyyy HH:mm:ss", "MM/yyyy HH:mm:ss", "yyyy-MM-dd'T'HH:mm", 
                                                            "yyyy-MM-dd HH:mm", "yyyy/MM/dd HH:mm", "yyyy-MM-dd HH:mm", "yyyy/MM/dd HH:mm", "MM-dd-yyyy HH:mm",
                                                            "MM/dd/yyyy HH:mm", "dd-MM-yyyy HH:mm", "dd/MM/yyyy HH:mm", "MM-yyyy HH:mm", "MM/yyyy HH:mm",
                                                            "yyyy-MM-dd'T'HH", "yyyy-MM-dd HH", "yyyy/MM/dd HH", "yyyy-MM-dd HH", "yyyy/MM/dd HH", 
                                                            "MM-dd-yyyy HH", "MM/dd/yyyy HH", "dd-MM-yyyy HH", "dd/MM/yyyy HH", "MM-yyyy HH", "MM/yyyy HH",
                                                            "yyyy-MM-dd", "yyyy/MM/dd", "MM-dd-yyyy", "MM/dd/yyyy", "dd-MM-yyyy", "dd/MM/yyyy", "MM-yyyy",
                                                            "MM/yyyy", "yyyy-M-dd'T'HH:m:ss", "yyyy-M-dd HH:m:ss", "yyyy/M/dd HH:m:ss", "yyyy-M-dd HH:m:ss", 
                                                            "yyyy/M/dd HH:m:ss", "M-dd-yyyy HH:m:ss", "M/dd/yyyy HH:m:ss", "dd-M-yyyy HH:m:ss", "dd/M/yyyy HH:m:ss",
                                                            "M-yyyy HH:m:ss", "M/yyyy HH:m:ss", "yyyy-M-dd'T'HH:m", "yyyy-M-dd HH:m", "yyyy/M/dd HH:m", 
                                                            "yyyy-M-dd HH:m", "yyyy/M/dd HH:m", "M-dd-yyyy HH:m", "M/dd/yyyy HH:m", "dd-M-yyyy HH:m",
                                                            "dd/M/yyyy HH:m", "M-yyyy HH:m", "M/yyyy HH:m", "yyyy-M-dd'T'HH", "yyyy-M-dd HH", "yyyy/M/dd HH", 
                                                            "yyyy-M-dd HH", "yyyy/M/dd HH", "M-dd-yyyy HH", "M/dd/yyyy HH", "dd-M-yyyy HH", "dd/M/yyyy HH",
                                                            "M-yyyy HH", "M/yyyy HH", "yyyy-M-dd", "yyyy/M/dd", "M-dd-yyyy", "M/dd/yyyy", "dd-M-yyyy",
                                                            "dd/M/yyyy", "M-yyyy", "M/yyyy", "yyyy-M-d'T'HH:m:ss", "yyyy-M-d HH:m:ss", "yyyy/M/d HH:m:ss", 
                                                            "yyyy-M-d HH:m:ss", "yyyy/M/d HH:m:ss", "M-d-yyyy HH:m:ss", "M/d/yyyy HH:m:ss", "d-M-yyyy HH:m:ss",
                                                            "d/M/yyyy HH:m:ss", "M-yyyy HH:m:ss", "M/yyyy HH:m:ss", "yyyy-M-d'T'HH:m", "yyyy-M-d HH:m", 
                                                            "yyyy/M/d HH:m", "yyyy-M-d HH:m", "yyyy/M/d HH:m", "M-d-yyyy HH:m", "M/d/yyyy HH:m", "d-M-yyyy HH:m",
		                                                    "d/M/yyyy HH:m", "M-yyyy HH:m", "M/yyyy HH:m", "yyyy-M-d'T'HH", "yyyy-M-d HH", "yyyy/M/d HH", 
                                                            "yyyy-M-d HH", "yyyy/M/d HH", "M-d-yyyy HH", "M/d/yyyy HH", "d-M-yyyy HH", "d/M/yyyy HH", "M-yyyy HH",
                                                            "M/yyyy HH", "yyyy-M-d", "yyyy/M/d", "M-d-yyyy", "M/d/yyyy", "d-M-yyyy", "d/M/yyyy", "M-yyyy",
                                                            "M/yyyy", "yyyy-MM-d'T'HH:mm:ss", "yyyy-MM-d HH:mm:ss", "yyyy/MM/d HH:mm:ss", "yyyy-MM-d HH:mm:ss", 
                                                            "yyyy/MM/d HH:mm:ss", "MM-d-yyyy HH:mm:ss", "MM/d/yyyy HH:mm:ss", "d-MM-yyyy HH:mm:ss",
                                                            "d/MM/yyyy HH:mm:ss", "MM-yyyy HH:mm:ss", "MM/yyyy HH:mm:ss", "yyyy-MM-d'T'HH:mm", "yyyy-MM-d HH:mm", 
                                                            "yyyy/MM/d HH:mm", "yyyy-MM-d HH:mm", "yyyy/MM/d HH:mm", "MM-d-yyyy HH:mm", "MM/d/yyyy HH:mm",
                                                            "d-MM-yyyy HH:mm", "d/MM/yyyy HH:mm", "MM-yyyy HH:mm", "MM/yyyy HH:mm", "yyyy-MM-d'T'HH", 
                                                            "yyyy-MM-d HH", "yyyy/MM/d HH", "yyyy-MM-d HH", "yyyy/MM/d HH", "MM-d-yyyy HH", "MM/d/yyyy HH",
                                                            "d-MM-yyyy HH", "d/MM/yyyy HH", "MM-yyyy HH", "MM/yyyy HH", "yyyy-MM-d", "yyyy/MM/d", "MM-d-yyyy",
                                                            "MM/d/yyyy", "d-MM-yyyy", "d/MM/yyyy", "MM-yyyy", "MM/yyyy", "EEE, d MMM yyyy HH:mm:ss",
                                                            "d MMM yyyy HH:mm:ss", "d MMM yyyy", "MMM d, yyyy", "MMM dd, yyyy", "yyyy/MM", "yyyy/M",
                                                            "yyyy-MM", "yyyy-M"};
	// @formatter:on
	
	private StringUtilities() {
	}

	public static boolean isNotNullOrEmpty(String value) {
		return !isNullOrEmpty(value);
	}

	public static boolean isNullOrEmpty(String value) {
		return (value == null) || value.isEmpty();
	}

	public static boolean isFormattedDate(String value) {
		// Check for ISO 8601 Date Format (yyyy-MM-ddTHH:mm:ss)
		if (isNotNullOrEmpty(value) && (value.length() > 14)) {
			String sCheck = value.substring(4, 5) + value.substring(7, 8) + value.substring(10, 11) + value.substring(13, 14);
			if (sCheck.equals("--T:") && isDate(value)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isDate(String value) {
		try {
			DateUtils.parseDateStrictly(value, SUPPORTED_DATE_FORMATS);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	public static boolean isLong(String value) {
		if (isNullOrEmpty(value)) return false;
		value = value.trim();
		return (Pattern.matches("^[+-]?\\d+$", value) && (value.length() <= 17));
	}

	public static boolean isInteger(String value) {
		if (isNullOrEmpty(value)) return false;
		value = value.trim();
		return (Pattern.matches("^[+-]?\\d+$", value) && (value.length() <= 8));
	}

	public static boolean isDouble(String value) {
		if (isNullOrEmpty(value)) return false;
		value = value.trim();
		if (value.indexOf('.') == -1) return false;
		return Pattern.matches(DOUBLE_REGEX, value);
	}

	public static boolean isBigDecimal(String value) {
		if (isNullOrEmpty(value)) return false;
		value = value.trim();
		if (value.indexOf('.') == -1) return false;
		return Pattern.matches(DOUBLE_REGEX, value);
	}

//	public static boolean isFormula(String value) {
//		if (isNullOrEmpty(value)) return false;
//		char[] aSymbols = ".*/+-()=<>!^#&@$%\\|{}'?".toCharArray();
//		for (int i = 0; i < aSymbols.length; i++) {
//			if (value.indexOf(aSymbols[i]) != -1) {
//				return true;
//			}
//		}
//		return false;
//	}

	public static boolean isBoolean(String value) {
		return (isNullOrEmpty(value) || (BOOLEAN_VALUES.indexOf("|" + value.trim().toLowerCase() + "|") == -1)) ? false : true;
	}

	public static boolean toBoolean(String value) {
		return toBoolean(value, false);
	}

	public static boolean toBoolean(String value, Boolean defaultValue) {
		if (isNullOrEmpty(value))
			return defaultValue;
		else if ("|true|t|y|1|on|yes|".indexOf("|" + value.trim().toLowerCase() + "|") > -1)
			return true;
		else if ("|false|f|n|0|off|no|".indexOf("|" + value.trim().toLowerCase() + "|") > -1)
			return false;
		return defaultValue;
	}

	public static int toInteger(String value) {
		return toInteger(value, 0);
	}

	public static int toInteger(String value, int defaultValue) {
		if (isNullOrEmpty(value))
			return defaultValue;
		try {
			return Integer.parseInt(value.trim());
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
			return Long.parseLong(value.trim());
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
			return Double.parseDouble(value.trim());
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
			return new BigDecimal(value.trim());
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}

	public static Date toDate(String value) {
		return toDate(value, null);
	}

	public static Date toDate(String value, Date defaultValue) {
		try {
			return DateUtils.parseDateStrictly(value, SUPPORTED_DATE_FORMATS);
		} catch (Exception ex) {
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
		
		if ((value.startsWith("\"") && value.endsWith("\""))) 
			return value;
		
		return String.format("\"%s\"", value);
	}

	public static String getDataType(String value, String previousType) {
		if (isNullOrEmpty(value)) {
			return previousType;
		} else if (isNullOrEmpty(previousType)) {
			if (isBoolean(value)) {
				return "BooleanData";
			} else if (isDate(value)) {
				return "DateData";
			} else if (isInteger(value)) {
				return "IntegerData";
			} else if (isLong(value)) {
				return "LongData";
			} else if (isDouble(value)) {
				return "DoubleData";
			} else if (isBigDecimal(value)) {
				return "BigDecimal";
			} else {
				return "StringData";
			}
		} else if (previousType.equals("StringData")) {
			return previousType;
		} else if (previousType.equals("BooleanData")) {
			return isBoolean(value) ? "BooleanData" : "StringData";
		} else if (previousType.equals("DateData")) {
			return isDate(value) ? previousType : "StringData";
		} else if (previousType.equals("IntegerData") && isInteger(value)) {
			return previousType;
		} else if ((previousType.equals("IntegerData") || previousType.equals("LongData")) && isLong(value)) {
			return "LongData";
		} else if (("|IntegerData|LongData|DoubleData|".indexOf("|" + previousType + "|") > -1) && isDouble(value)) {
			return "DoubleData";
		} else if ("|IntegerData|LongData|DoubleData|BigDecimalData".indexOf("|" + previousType + "|") > -1) {
			return isBigDecimal(value) ? "BigDecimalData" : "StringData";
		} else if (isNotNullOrEmpty(previousType)) {
			return "StringData";
		} else {
			throw new RuntimeException("Unable to detect delimited file schema format.");
		}
	}

	public static String[] split(String value) {
		return split(value, ",");
	}

	public static String[] split(String value, String regex) {
		if (isNullOrEmpty(value))
			return new String[0];

		String[] values = value.split(regex);
		int length = values.length;

		for (int i = 0; i < length; i++) {
			values[i] = values[i].trim();
		}
		return values;
	}
	
	public static int periodToSeconds(String value) {
		if (StringUtilities.isNullOrEmpty(value))
			return -1;

		char cUnits = 's';

		int iStart = 0;
		int iSeconds = 0;
		int iPosition = 0;
		int iCurrentValue = 0;

		value = value.toLowerCase();
		String[] aNumbers = value.split("d|h|m|s");
		for (int i = 0; i < aNumbers.length; i++) {
			if (StringUtilities.isNullOrEmpty(aNumbers[i]))
				continue;

			iPosition = value.indexOf(aNumbers[i], iStart) + aNumbers[i].length();
			iStart = iPosition + 1;
			cUnits = (iPosition < value.length()) ? value.charAt(iPosition) : 's';
			iCurrentValue = StringUtilities.toInteger(aNumbers[i], 0);

			switch (cUnits) {
			case 'd':
				iSeconds += iCurrentValue * 86400;
				break;
			case 'h':
				iSeconds += iCurrentValue * 3600;
				break;
			case 'm':
				iSeconds += iCurrentValue * 60;
				break;
			case 's':
				iSeconds += iCurrentValue;
				break;
			}
		}
		return (iSeconds < 1) ? -1 : iSeconds;
	}
}
