package com.fanniemae.devtools.pie.common;

import org.junit.Test;

import junit.framework.TestCase;

/**
*
* @author Richard Monson
* @since 2017-01-05
* 
*/

public class StringUtilitiesTest extends TestCase {

	//***********************************************************************************
	//* isNotNullOrEmpty 
	//***********************************************************************************
	
	@Test
	public void testIsNotNullOrEmptySimple() {
		assertEquals("String is not null or empty",true,StringUtilities.isNotNullOrEmpty("Test"));
	}
	
	@Test
	public void testIsNotNullOrEmptyNull() {
		assertEquals("String is null",false,StringUtilities.isNotNullOrEmpty(null));
	}
	
	@Test
	public void testIsNotNullOrEmptyEmptyString() {
		assertEquals("String is empty",false,StringUtilities.isNotNullOrEmpty(""));
	}

	@Test
	public void testIsNotNullOrEmptySpaces() {
		assertEquals("String is spaces",true,StringUtilities.isNotNullOrEmpty("   "));
	}
	
	@Test
	public void testIsNotNullOrEmptyPaddedString() {
		assertEquals("String is padded",true,StringUtilities.isNotNullOrEmpty(" sdfsdfsd  "));
	}
	
	@Test
	public void testIsNotNullOrEmptyLeftPaddedString() {
		assertEquals("String is left padded",true,StringUtilities.isNotNullOrEmpty(" sdfsdfsd"));
	}
	
	@Test
	public void testIsNotNullOrEmptyRightPaddedString() {
		assertEquals("String is right padded",true,StringUtilities.isNotNullOrEmpty("sdfsdfsd   "));
	}
	
	//***********************************************************************************
	//* isNullOrEmpty 
	//***********************************************************************************	
	
	@Test
	public void testIsNullOrEmptySimple() {
		assertEquals("String is not null or empty",false,StringUtilities.isNullOrEmpty("Test"));
	}
	
	@Test
	public void testIsNullOrEmptyNull() {
		assertEquals("String is null",true,StringUtilities.isNullOrEmpty(null));
	}
	
	@Test
	public void testIsNullOrEmptyEmptyString() {
		assertEquals("String is empty",true,StringUtilities.isNullOrEmpty(""));
	}

	@Test
	public void testIsNullOrEmptySpaces() {
		assertEquals("String is spaces",false,StringUtilities.isNullOrEmpty("   "));
	}
	
	@Test
	public void testIsNullOrEmptyPaddedString() {
		assertEquals("String is padded",false,StringUtilities.isNullOrEmpty(" sdfsdfsd  "));
	}
	
	@Test
	public void testIsNullOrEmptyLeftPaddedString() {
		assertEquals("String is left padded",false,StringUtilities.isNullOrEmpty(" sdfsdfsd"));
	}
	
	@Test
	public void testIsNullOrEmptyRightPaddedString() {
		assertEquals("String is right padded",false,StringUtilities.isNullOrEmpty("sdfsdfsd   "));
	}	
	
	//***********************************************************************************
	//* isFormattedDate 
	//***********************************************************************************
	
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

	//***********************************************************************************
	//* isDate 
	//***********************************************************************************
	
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

	//***********************************************************************************
	//* isLong 
	//***********************************************************************************
	
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
	
	//***********************************************************************************
	//* isInteger 
	//***********************************************************************************

//	@Test
//	public void testIsInteger() {
//		fail("Not yet implemented");
//	}

	//***********************************************************************************
	//* isDouble 
	//***********************************************************************************
	
//	@Test
//	public void testIsDouble() {
//		fail("Not yet implemented");
//	}

	//***********************************************************************************
	//* isBigDecimal 
	//***********************************************************************************
	
//	@Test
//	public void testIsBigDecimal() {
//		fail("Not yet implemented");
//	}

	//***********************************************************************************
	//* isFormula 
	//***********************************************************************************
	
//	@Test
//	public void testIsFormula() {
//		fail("Not yet implemented");
//	}

	//***********************************************************************************
	//* isBoolean 
	//***********************************************************************************
	
//	@Test
//	public void testIsBoolean() {
//		fail("Not yet implemented");
//	}

	//***********************************************************************************
	//* toBoolean 
	//***********************************************************************************
	
//	@Test
//	public void testToBooleanString() {
//		fail("Not yet implemented");
//	}

	//***********************************************************************************
	//* toBoolean with default 
	//***********************************************************************************
	
//	@Test
//	public void testToBooleanStringBoolean() {
//		fail("Not yet implemented");
//	}

	//***********************************************************************************
	//* toInteger  
	//***********************************************************************************
	
//	@Test
//	public void testToIntegerString() {
//		fail("Not yet implemented");
//	}

	//***********************************************************************************
	//* toInteger with default  
	//***********************************************************************************
	
//	@Test
//	public void testToIntegerStringInt() {
//		fail("Not yet implemented");
//	}

	//***********************************************************************************
	//* toLong  
	//***********************************************************************************
	
//	@Test
//	public void testToLongString() {
//		fail("Not yet implemented");
//	}

	//***********************************************************************************
	//* toLong with default  
	//***********************************************************************************
	
//	@Test
//	public void testToLongStringLong() {
//		fail("Not yet implemented");
//	}

	//***********************************************************************************
	//* toDouble  
	//***********************************************************************************
	
//	@Test
//	public void testToDoubleString() {
//		fail("Not yet implemented");
//	}

	//***********************************************************************************
	//* toDouble with default  
	//***********************************************************************************
	
//	@Test
//	public void testToDoubleStringDouble() {
//		fail("Not yet implemented");
//	}

	//***********************************************************************************
	//* toBigDecimal  
	//***********************************************************************************
	
//	@Test
//	public void testToBigDecimalString() {
//		fail("Not yet implemented");
//	}

	//***********************************************************************************
	//* toBigDecimal with default  
	//***********************************************************************************
	
//	@Test
//	public void testToBigDecimalStringBigDecimal() {
//		fail("Not yet implemented");
//	}

	//***********************************************************************************
	//* toDate  
	//***********************************************************************************
	
//	@Test
//	public void testToDateString() {
//		fail("Not yet implemented");
//	}

	//***********************************************************************************
	//* toDate with default
	//***********************************************************************************
	
//	@Test
//	public void testToDateStringDate() {
//		fail("Not yet implemented");
//	}

	//***********************************************************************************
	//*  
	//***********************************************************************************
	
//	@Test
//	public void testToObject() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testWrapValue() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testGetDataType() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testSplitString() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testSplitStringString() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testPeriodToSeconds() {
//		fail("Not yet implemented");
//	}

}
