package com.fanniemae.devtools.pie.common;

import org.junit.Test;

import junit.framework.TestCase;

import com.fanniemae.devtools.pie.common.ArrayUtilities;

/**
*
* @author Richard Monson
* @since 2017-01-05
* 
*/

public class ArrayUtilitiesTest extends TestCase {

	protected static String[][] _itemsNull = null;
	protected static String[][] _itemsWithNull = new String[][] { { "Dog", "Black" }, { "Cat", null }, { null, "Gray" }, { "Horse", "Brown" } };
	protected static String[][] _itemsWithEmptyStrings = new String[][] { { "Dog", "Black" }, { "Cat", "" }, { "", "Gray" }, { "Horse", "Brown" } };
	protected static String[][] _itemsWithNullEntry = new String[][] { { "Dog", "Black" }, null, { "Cat", null }, { null, "Gray" }, { "Horse", "Brown" } };

	//***********************************************************************************
	//* Test cases for simplest 2 parameter method call 
	//***********************************************************************************
	
	@Test
	public void testSearchNullArraysForNull() {
		assertEquals("Search null array for null", -1, ArrayUtilities.indexOf(_itemsNull, null));
	}
	
	@Test
	public void testSearchNullArrayForValue() {
		assertEquals("Search null array for string", -1, ArrayUtilities.indexOf(_itemsNull, "dog"));
	}
	
	@Test
	public void testSearchArrayForNullFound() {
		assertEquals("Search for null in array with null", 2, ArrayUtilities.indexOf(_itemsWithNull, null));		
	}
	
	@Test 
	public void testSearchArrayForNullNotFound() {
		assertEquals("Search for null in array without null", -1, ArrayUtilities.indexOf(_itemsWithEmptyStrings, null));		
	}	
	
	@Test
	public void testSearchArrayWithNullEntryForNull() {
		assertEquals("Search for null in array with null entry", 3, ArrayUtilities.indexOf(_itemsWithNullEntry, null));
	}
	
	@Test
	public void testSearchArrayForNullValueFound() {
		assertEquals("Search for null in array with empty strings", -1, ArrayUtilities.indexOf(_itemsWithEmptyStrings, null));		
	}
	
	@Test
	public void testSearchArrayForWrongCaseValueFound() {
		assertEquals("Search array for string", 4, ArrayUtilities.indexOf(_itemsWithNullEntry, "horse"));
	}
	
	@Test
	public void testSearchArrayForRightCaseValueFound() {
		assertEquals("Search array for string", 4, ArrayUtilities.indexOf(_itemsWithNullEntry, "Horse"));
	}

	//***********************************************************************************
	//* Test cases for method call with ignore case
	//***********************************************************************************
	
	@Test
	public void testSearchForWrongCaseValueNotFound() {
		assertEquals("Search array for string", -1, ArrayUtilities.indexOf(_itemsWithNullEntry, "horse", false));
	}
	
	@Test
	public void testSearchForRightCaseValueFound() {
		assertEquals("Search array for string", 4, ArrayUtilities.indexOf(_itemsWithNullEntry, "Horse", false));
	}
	
	@Test
	public void testSearchForWrongCaseValueFound() {
		assertEquals("Search array for string", 4, ArrayUtilities.indexOf(_itemsWithNullEntry, "horse", true));
	}
	
	@Test
	public void testSearchForValueIgnoreCaseFound() {
		assertEquals("Search array for string", 4, ArrayUtilities.indexOf(_itemsWithNullEntry, "Horse", true));
	}
	
	//***********************************************************************************
	//* Test cases for method call with just dimension 
	//***********************************************************************************
	
	@Test
	public void testSearchNullArrayDimensionForNull() {
		assertEquals("Search null array dimension for null", -1, ArrayUtilities.indexOf(_itemsNull, null, 1));
	}
	
	@Test
	public void testSearchNullArrayDimensionForValue() {
		assertEquals("Search null array dimension for string", -1, ArrayUtilities.indexOf(_itemsNull, "dog", 1));
	}
	
	@Test
	public void testSearchArrayDimensionForNullFound() {
		assertEquals("Search for null in array dimension with null", 1, ArrayUtilities.indexOf(_itemsWithNull, null, 1));		
	}
	
	@Test 
	public void testSearchArrayDimensionForNullNotFound() {
		assertEquals("Search for null in array dimension without null", -1, ArrayUtilities.indexOf(_itemsWithEmptyStrings, null, 1));		
	}	
	
	@Test
	public void testSearchArrayDimensionWithNullEntryForNull() {
		assertEquals("Search for null in array dimension with null entry", 2, ArrayUtilities.indexOf(_itemsWithNullEntry, null, 1));
	}
	
	@Test
	public void testSearchArrayDimensionForNullValueFound() {
		assertEquals("Search for null in array dimension with empty strings", -1, ArrayUtilities.indexOf(_itemsWithEmptyStrings, null, 1));		
	}
	
	@Test
	public void testSearchArrayNegativeDimensionForValueFound() {
		assertEquals("Search array dimension -1 for string", -1, ArrayUtilities.indexOf(_itemsWithNullEntry, "brown", -1));
	}
	
	@Test
	public void testSearchArrayBeyondDimensionForValueFound() {
		assertEquals("Search beyond array dimension for string", -1, ArrayUtilities.indexOf(_itemsWithNullEntry, "Brown", 10000));
	}
	
	@Test
	public void testSearchArrayDimensionForWrongCaseValueFound() {
		assertEquals("Search array dimension for string", 4, ArrayUtilities.indexOf(_itemsWithNullEntry, "brown", 1));
	}
	
	@Test
	public void testSearchArrayDimensionForRightCaseValueFound() {
		assertEquals("Search array dimension for case matching string", 4, ArrayUtilities.indexOf(_itemsWithNullEntry, "Brown", 1));
	}	
	
}
