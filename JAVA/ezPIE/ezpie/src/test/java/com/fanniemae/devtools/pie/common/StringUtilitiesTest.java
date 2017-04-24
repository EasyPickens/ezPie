package com.fanniemae.devtools.pie.common;

import java.math.BigDecimal;

import org.junit.Test;

import com.fanniemae.ezpie.common.StringUtilities;

import junit.framework.TestCase;

/**
 *
 * @author Richard Monson
 * @since 2017-01-05
 * 
 */

public class StringUtilitiesTest extends TestCase {

	// ***********************************************************************************
	// * isNotNullOrEmpty
	// ***********************************************************************************

	@Test
	public void testIsNotNullOrEmptySimple() {
		assertEquals("String is not null or empty", true, StringUtilities.isNotNullOrEmpty("Test"));
	}

	@Test
	public void testIsNotNullOrEmptyNull() {
		assertEquals("String is null", false, StringUtilities.isNotNullOrEmpty(null));
	}

	@Test
	public void testIsNotNullOrEmptyEmptyString() {
		assertEquals("String is empty", false, StringUtilities.isNotNullOrEmpty(""));
	}

	@Test
	public void testIsNotNullOrEmptySpaces() {
		assertEquals("String is spaces", true, StringUtilities.isNotNullOrEmpty("   "));
	}

	@Test
	public void testIsNotNullOrEmptyPaddedString() {
		assertEquals("String is padded", true, StringUtilities.isNotNullOrEmpty(" sdfsdfsd  "));
	}

	@Test
	public void testIsNotNullOrEmptyLeftPaddedString() {
		assertEquals("String is left padded", true, StringUtilities.isNotNullOrEmpty(" sdfsdfsd"));
	}

	@Test
	public void testIsNotNullOrEmptyRightPaddedString() {
		assertEquals("String is right padded", true, StringUtilities.isNotNullOrEmpty("sdfsdfsd   "));
	}

	// ***********************************************************************************
	// * isNullOrEmpty
	// ***********************************************************************************

	@Test
	public void testIsNullOrEmptySimple() {
		assertEquals("String is not null or empty", false, StringUtilities.isNullOrEmpty("Test"));
	}

	@Test
	public void testIsNullOrEmptyNull() {
		assertEquals("String is null", true, StringUtilities.isNullOrEmpty(null));
	}

	@Test
	public void testIsNullOrEmptyEmptyString() {
		assertEquals("String is empty", true, StringUtilities.isNullOrEmpty(""));
	}

	@Test
	public void testIsNullOrEmptySpaces() {
		assertEquals("String is spaces", false, StringUtilities.isNullOrEmpty("   "));
	}

	@Test
	public void testIsNullOrEmptyPaddedString() {
		assertEquals("String is padded", false, StringUtilities.isNullOrEmpty(" sdfsdfsd  "));
	}

	@Test
	public void testIsNullOrEmptyLeftPaddedString() {
		assertEquals("String is left padded", false, StringUtilities.isNullOrEmpty(" sdfsdfsd"));
	}

	@Test
	public void testIsNullOrEmptyRightPaddedString() {
		assertEquals("String is right padded", false, StringUtilities.isNullOrEmpty("sdfsdfsd   "));
	}

	// ***********************************************************************************
	// * isFormattedDate
	// ***********************************************************************************

	@Test
	public void testIsFormattedDate() {
		assertEquals("Formatted date test for 2010-12-12T14:03:34", true, StringUtilities.isFormattedDate("2010-12-12T14:03:34"));
	}

	@Test
	public void testIsFormattedDateBadDate() {
		assertEquals("Formatted date test for 2010-35-32T14:03:34", false, StringUtilities.isFormattedDate("2010-35-32T14:03:34"));
	}

	@Test
	public void testIsFormattedDateIncompleteDate() {
		assertEquals("Formatted date test for 2010-12-22", false, StringUtilities.isFormattedDate("2010-12-22"));
	}

	@Test
	public void testIsFormattedDateEmptyString() {
		assertEquals("Formatted date test for empty string", false, StringUtilities.isFormattedDate(""));
	}

	@Test
	public void testIsFormattedDateNull() {
		assertEquals("Formatted date test for null", false, StringUtilities.isFormattedDate(null));
	}

	// ***********************************************************************************
	// * isDate
	// ***********************************************************************************

	@Test
	public void testIsDateISO() {
		assertEquals("IsDate with 2010-12-12T14:03:34", true, StringUtilities.isDate("2010-12-12T14:03:34"));
	}

	@Test
	public void testIsDateBadISO() {
		assertEquals("IsDate with 2010-55-12T14:03:34", false, StringUtilities.isDate("2010-55-12T14:04:34"));
	}

	@Test
	public void testIsDateNull() {
		assertEquals("IsDate with null", false, StringUtilities.isDate(null));
	}

	@Test
	public void testIsDateEmptyString() {
		assertEquals("IsDate with empty string", false, StringUtilities.isDate(""));
	}

	@Test
	public void testIsDateOnlyDate() {
		assertEquals("IsDate with 2016-02-21", true, StringUtilities.isDate("2016-02-21"));
	}

	@Test
	public void testIsDateYearMonth() {
		assertEquals("IsDate with 2016-02", true, StringUtilities.isDate("2016-02"));
	}

	@Test
	public void testIsDateInvalidYearMonth() {
		assertEquals("IsDate with 2016-45", false, StringUtilities.isDate("2016-45"));
	}

	@Test
	public void testIsDateYearSlashMonth() {
		assertEquals("IsDate with 2016/02", true, StringUtilities.isDate("2016/02"));
	}

	@Test
	public void testIsDateYearSlashMonthSlashDay() {
		assertEquals("IsDate with 2016/02/23", true, StringUtilities.isDate("2016/02/23"));
	}

	@Test
	public void testIsDateInvalidYearSlashMonthSlashDay() {
		assertEquals("IsDate with 2016/14/23", false, StringUtilities.isDate("2016/14/23"));
	}

	// ***********************************************************************************
	// * isLong
	// ***********************************************************************************

	@Test
	public void testIsLong() {
		assertEquals("IsLong with 23423425252", true, StringUtilities.isLong("23423425252"));
	}

	@Test
	public void testIsLongNull() {
		assertEquals("IsLong with null", false, StringUtilities.isLong(null));
	}

	@Test
	public void testIsLongEmpty() {
		assertEquals("IsLong with empty string", false, StringUtilities.isLong(""));
	}

	@Test
	public void testIsLongSpaces() {
		assertEquals("IsLong with spaces", false, StringUtilities.isLong("  "));
	}

	@Test
	public void testIsLongPadded() {
		assertEquals("IsLong with padding", true, StringUtilities.isLong(" 234525252 "));
	}

	@Test
	public void testIsLongRightPadded() {
		assertEquals("IsLong with right padding", true, StringUtilities.isLong("234525252  "));
	}

	@Test
	public void testIsLongleftPadded() {
		assertEquals("IsLong with left padding", true, StringUtilities.isLong("   234525252"));
	}

	@Test
	public void testIsLongDecimals() {
		assertEquals("IsLong with decimal", false, StringUtilities.isLong(" 7593.987"));
	}

	@Test
	public void testIsLongText() {
		assertEquals("IsLong with hello 34", false, StringUtilities.isLong("hello 34"));
	}

	@Test
	public void testIsLongTextNumFirst() {
		assertEquals("IsLong with 34hello", false, StringUtilities.isLong("34hello"));
	}

	// ***********************************************************************************
	// * isInteger
	// ***********************************************************************************

	@Test
	public void testIsInteger() {
		isInteger(null, false);
	}
	
	@Test
	public void testIsIntegerEmptyString() {
		isInteger("", false);
	}
	
	@Test
	public void testIsIntegerEmptySpace() {
		isInteger("  ", false);
	}
	
	@Test
	public void testIsIntegerText() {
		isInteger("Hello", false);
	}
	
	@Test
	public void testIsIntegerNumber() {
		isInteger("345", true);
	}
	
	@Test
	public void testIsIntegerPaddingNumber() {
		isInteger("  345 ", true);
	}
	
	@Test
	public void testIsIntegerLeftPadding() {
		isInteger("   345", true);
	}	
	
	@Test
	public void testIsIntegerRightPadding() {
		isInteger("345  ", true);
	}
	
	@Test
	public void testIsIntegerLeadingZeros() {
		isInteger("0000345", true);
	}
	
	@Test
	public void testIsIntegerDecimal() {
		isInteger("123.456", false);
	}

	private void isInteger(String value, boolean expectedResult) {
		if (value == null) {
			assertEquals("IsInteger with null", expectedResult, StringUtilities.isInteger(value));
		} else {
			assertEquals("IsInteger with ", expectedResult, StringUtilities.isInteger(value));
		}
	}

	// ***********************************************************************************
	// * isDouble
	// ***********************************************************************************

	@Test
	public void testIsDouble() {
		isDouble(null, false);
	}
	
	@Test
	public void testIsDoubleEmptyString() {
		isDouble("", false);
	}
	
	@Test
	public void testIsDoubleEmptySpace() {
		isDouble("  ", false);
	}
	
	@Test
	public void testIsDoubleText() {
		isDouble("Hello", false);
	}
	
	@Test
	public void testIsDoubleNumber() {
		isDouble("345.678", true);
	}
	
	@Test
	public void testIsDoublePaddingNumber() {
		isDouble("  345.678 ", true);
	}
	
	@Test
	public void testIsDoubleLeftPadding() {
		isDouble("   345.678", true);
	}	
	
	@Test
	public void testIsDoubleRightPadding() {
		isDouble("345.678  ", true);
	}
	
	@Test
	public void testIsDoubleLeadingZeros() {
		isDouble("0000345.678", true);
	}
	
	@Test
	public void testIsDoubleInt() {
		isDouble("123", false);
	}

	private void isDouble(String value, boolean expectedResult) {
		if (value == null) {
			assertEquals("IsDouble with null", expectedResult, StringUtilities.isDouble(value));
		} else {
			assertEquals("IsDouble with ", expectedResult, StringUtilities.isDouble(value));
		}
	}

	// ***********************************************************************************
	// * isBigDecimal
	// ***********************************************************************************
	@Test
	public void testIsBigDecimal() {
		isBigDecimal(null, false);
	}
	
	@Test
	public void testIsBigDecimalEmptyString() {
		isBigDecimal("", false);
	}
	
	@Test
	public void testIsBigDecimalEmptySpace() {
		isBigDecimal("  ", false);
	}
	
	@Test
	public void testIsBigDecimalText() {
		isBigDecimal("Hello", false);
	}
	
	@Test
	public void testIsBigDecimalNumber() {
		isBigDecimal("345.678", true);
	}
	
	@Test
	public void testIsBigDecimalPaddingNumber() {
		isBigDecimal("  345.678 ", true);
	}
	
	@Test
	public void testIsBigDecimalLeftPadding() {
		isBigDecimal("   345.678", true);
	}	
	
	@Test
	public void testIsBigDecimalRightPadding() {
		isBigDecimal("345.678  ", true);
	}
	
	@Test
	public void testIsBigDecimalLeadingZeros() {
		isBigDecimal("0000345.678", true);
	}
	
	@Test
	public void testIsBigDecimalInt() {
		isBigDecimal("123", false);
	}

	private void isBigDecimal(String value, boolean expectedResult) {
		if (value == null) {
			assertEquals("IsBigDecimal with null", expectedResult, StringUtilities.isBigDecimal(value));
		} else {
			assertEquals("IsBigDecimal with ", expectedResult, StringUtilities.isBigDecimal(value));
		}
	}

	// ***********************************************************************************
	// * isFormula
	// ***********************************************************************************

	// @Test
	// public void testIsFormula() {
	// fail("Not yet implemented");
	// }

	// ***********************************************************************************
	// * isBoolean
	// ***********************************************************************************
	@Test
	public void testIsBoolean() {
		isBoolean(null, false);
	}
	
	@Test
	public void testIsBooleanEmptyString() {
		isBoolean("", false);
	}
	
	@Test
	public void testIsBooleanEmptySpace() {
		isBoolean("  ", false);
	}
	
	@Test
	public void testIsBooleanText() {
		isBoolean("Hello", false);
	}
	
	@Test
	public void testIsBooleanNumber() {
		isBoolean("yes", true);
	}
	
	@Test
	public void testIsBooleanPadding() {
		isBoolean("  yes ", true);
	}
	
	@Test
	public void testIsBooleanLeftPadding() {
		isBoolean("   yes", true);
	}	
	
	@Test
	public void testIsBooleanRightPadding() {
		isBoolean("yes  ", true);
	}
	
	@Test
	public void testIsBooleanLeadingZeros() {
		isBoolean("0000yes", false);
	}
	
	@Test
	public void testIsBooleanInt() {
		isBoolean("123", false);
	}

	private void isBoolean(String value, boolean expectedResult) {
		if (value == null) {
			assertEquals("IsBoolean with null", expectedResult, StringUtilities.isBoolean(value));
		} else {
			assertEquals("IsBoolean with ", expectedResult, StringUtilities.isBoolean(value));
		}
	}
	// ***********************************************************************************
	// * toBoolean
	// ***********************************************************************************
	@Test
	public void testtoBoolean() {
		toBoolean(null, false);
	}
	
	@Test
	public void testtoBooleanEmptyString() {
		toBoolean("", false);
	}
	
	@Test
	public void testtoBooleanEmptySpace() {
		toBoolean("  ", false);
	}
	
	@Test
	public void testtoBooleanText() {
		toBoolean("Hello", false);
	}
	
	@Test
	public void testtoBooleanNumber() {
		toBoolean("yes", true);
	}
	
	@Test
	public void testtoBooleanPadding() {
		toBoolean("  yes ", true);
	}
	
	@Test
	public void testtoBooleanLeftPadding() {
		toBoolean("   yes", true);
	}	
	
	@Test
	public void testtoBooleanRightPadding() {
		toBoolean("yes  ", true);
	}
	
	@Test
	public void testtoBooleanLeadingZeros() {
		toBoolean("0000yes", false);
	}
	
	@Test
	public void testtoBooleanInt() {
		toBoolean("123", false);
	}

	private void toBoolean(String value, boolean expectedResult) {
		if (value == null) {
			assertEquals("toBoolean with null", expectedResult, StringUtilities.toBoolean(value));
		} else {
			assertEquals("toBoolean with ", expectedResult, StringUtilities.toBoolean(value));
		}
	}
	// ***********************************************************************************
	// * toBoolean with default
	// ***********************************************************************************

	@Test
	public void testtoBooleanDefaultFalse() {
		assertEquals("toBoolean with ", false, StringUtilities.toBoolean("Dog",false));
	}
	
	@Test
	public void testtoBooleanDefaultTrue() {
		assertEquals("toBoolean with ", true, StringUtilities.toBoolean("Dog",true));
	}

	// ***********************************************************************************
	// * toInteger
	// ***********************************************************************************
	@Test
	public void testtoInteger() {
		toInteger(null, 0);
	}
	
	@Test
	public void testtoIntegerEmptyString() {
		toInteger("", 0);
	}
	
	@Test
	public void testtoIntegerEmptySpace() {
		toInteger("  ", 0);
	}
	
	@Test
	public void testtoIntegerText() {
		toInteger("Hello", 0);
	}
	
	@Test
	public void testtoIntegerPadding() {
		toInteger("  -25 ", -25);
	}
	
	@Test
	public void testtoIntegerLeftPadding() {
		toInteger("   +25", 25);
	}	
	
	@Test
	public void testtoIntegerRightPadding() {
		toInteger("25  ", 25);
	}
	
	@Test
	public void testtoIntegerLeadingZeros() {
		toInteger("000025", 25);
	}
	
	private void toInteger(String value, int expectedResult) {
		if (value == null) {
			assertEquals("toInteger with null", expectedResult, StringUtilities.toInteger(value));
		} else {
			assertEquals("toInteger with ", expectedResult, StringUtilities.toInteger(value));
		}
	}
	// ***********************************************************************************
	// * toInteger with default
	// ***********************************************************************************

	@Test
	public void testtoIntegerDefaultFalse() {
		assertEquals("toInteger with ", 0, StringUtilities.toInteger("Dog",0));
	}
	
	@Test
	public void testtoIntegerDefaultTrue() {
		assertEquals("toInteger with ", 12, StringUtilities.toInteger("Dog",12));
	}
	// ***********************************************************************************
	// * toLong
	// ***********************************************************************************
	@Test
	public void testtoLong() {
		toLong(null, 0L);
	}
	
	@Test
	public void testtoLongEmptyString() {
		toLong("", 0L);
	}
	
	@Test
	public void testtoLongEmptySpace() {
		toLong("  ", 0L);
	}
	
	@Test
	public void testtoLongText() {
		toLong("Hello", 0L);
	}
	
	@Test
	public void testtoLongPadding() {
		toLong("  -25 ", -25);
	}
	
	@Test
	public void testtoLongLeftPadding() {
		toLong("   +25", 25);
	}	
	
	@Test
	public void testtoLongRightPadding() {
		toLong("25  ", 25);
	}
	
	@Test
	public void testtoLongLeadingZeros() {
		toLong("000025", 25);
	}
	
	private void toLong(String value, long expectedResult) {
		if (value == null) {
			assertEquals("toLong with null", expectedResult, StringUtilities.toLong(value));
		} else {
			assertEquals("toLong with ", expectedResult, StringUtilities.toLong(value));
		}
	}
	// ***********************************************************************************
	// * toLong with default
	// ***********************************************************************************

	@Test
	public void testtoLongDefaultFalse() {
		assertEquals("toLong with ", 0L, StringUtilities.toLong("Dog",0L));
	}
	
	@Test
	public void testtoLongDefaultTrue() {
		assertEquals("toLong with ", 12L, StringUtilities.toLong("Dog",12L));
	}
	// ***********************************************************************************
	// * toDouble
	// ***********************************************************************************
	@Test
	public void testtoDouble() {
		toDouble(null, 0.0);
	}
	
	@Test
	public void testtoDoubleEmptyString() {
		toDouble("", 0.0);
	}
	
	@Test
	public void testtoDoubleEmptySpace() {
		toDouble("  ", 0.0);
	}
	
	@Test
	public void testtoDoubleText() {
		toDouble("Hello", 0.0);
	}
	
	@Test
	public void testtoDoublePadding() {
		toDouble("  -25 ", -25);
	}
	
	@Test
	public void testtoDoubleLeftPadding() {
		toDouble("   +25", 25);
	}	
	
	@Test
	public void testtoDoubleRightPadding() {
		toDouble("25  ", 25);
	}
	
	@Test
	public void testtoDoubleLeadingZeros() {
		toDouble("000025", 25);
	}
	
	private void toDouble(String value, double expectedResult) {
		if (value == null) {
			assertEquals("toDouble with null", expectedResult, StringUtilities.toDouble(value));
		} else {
			assertEquals("toDouble with ", expectedResult, StringUtilities.toDouble(value));
		}
	}
	// ***********************************************************************************
	// * toDouble with default
	// ***********************************************************************************
	@Test
	public void testtoDoubleDefaultFalse() {
		assertEquals("toDouble with ", 0.0, StringUtilities.toDouble("Dog",0.0));
	}
	
	@Test
	public void testtoDoubleDefaultTrue() {
		assertEquals("toDouble with ", 1.23, StringUtilities.toDouble("Dog",1.23));
	}
	// ***********************************************************************************
	// * toBigDecimal
	// ***********************************************************************************
	@Test
	public void testtoBigDecimal() {
		toBigDecimal(null, new BigDecimal("0.0"));
	}
	
	@Test
	public void testtoBigDecimalEmptyString() {
		toBigDecimal("", new BigDecimal("0.0"));
	}
	
	@Test
	public void testtoBigDecimalEmptySpace() {
		toBigDecimal("  ", new BigDecimal("0.0"));
	}
	
	@Test
	public void testtoBigDecimalText() {
		toBigDecimal("Hello", new BigDecimal("0.0"));
	}
	
	@Test
	public void testtoBigDecimalPadding() {
		toBigDecimal("  -25 ",new BigDecimal("-25"));
	}
	
	@Test
	public void testtoBigDecimalLeftPadding() {
		toBigDecimal("   +25", new BigDecimal("25"));
	}	
	
	@Test
	public void testtoBigDecimalRightPadding() {
		toBigDecimal("25  ", new BigDecimal("25"));
	}
	
	@Test
	public void testtoBigDecimalLeadingZeros() {
		toBigDecimal("000025", new BigDecimal("25"));
	}
	
	private void toBigDecimal(String value, BigDecimal expectedResult) {
		if (value == null) {
			assertEquals("toBigDecimal with null", expectedResult, StringUtilities.toBigDecimal(value));
		} else {
			assertEquals("toBigDecimal with ", expectedResult, StringUtilities.toBigDecimal(value));
		}
	}
	// ***********************************************************************************
	// * toBigDecimal with default
	// ***********************************************************************************
	@Test
	public void testtoBigDecimalDefaultFalse() {
		assertEquals("toBigDecimal with ",new BigDecimal("0.0"), StringUtilities.toBigDecimal("Dog",new BigDecimal("0.0")));
	}
	
	@Test
	public void testtoBigDecimalDefaultTrue() {
		assertEquals("toBigDecimal with ",new BigDecimal("0.0001"), StringUtilities.toBigDecimal("Dog",new BigDecimal("0.0001")));
	}
	// ***********************************************************************************
	// * toDate
	// ***********************************************************************************

	// @Test
	// public void testToDateString() {
	// fail("Not yet implemented");
	// }

	// ***********************************************************************************
	// * toDate with default
	// ***********************************************************************************

	// @Test
	// public void testToDateStringDate() {
	// fail("Not yet implemented");
	// }

	// ***********************************************************************************
	// *
	// ***********************************************************************************

	// @Test
	// public void testToObject() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testWrapValue() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testGetDataType() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testSplitString() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testSplitStringString() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testPeriodToSeconds() {
	// fail("Not yet implemented");
	// }

}
