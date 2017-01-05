package com.fanniemae.devtools.pie.common;

import org.junit.Test;

import junit.framework.TestCase;

import com.fanniemae.devtools.pie.common.ArrayUtilities;

public class ArrayUtilitiesTest extends TestCase {

	protected static String[][] _itemsNull = null;
	protected static String[][] _itemsWithNull = new String[][] { { "Dog", "Black" }, { "Cat", null }, { null, "Gray" }, { "Horse", "Brown" } };
	protected static String[][] _itemsWithEmptyStrings = new String[][] { { "Dog", "Black" }, { "Cat", "" }, { "", "Gray" }, { "Horse", "Brown" } };
	protected static String[][] _itemsWithNullEntry = new String[][] { { "Dog", "Black" }, null, { null, "Gray" }, { "Horse", "Brown" } };

	protected static String[][] _itemsMixcase = new String[][] { { "Dog", "Black" }, { "Cat", "Orange" }, { "Mouse", "Gray" }, { "Horse", "Brown" } };
	protected static String[][] _itemsLowercase = new String[][] { { "dog", "black" }, { "cat", "orange" }, { "mouse", "gray" }, { "horse", "brown" } };
	protected static String[][] _itemsUppercase = new String[][] { { "DOG", "BLACK" }, { "CAT", "ORANGE" }, { "MOUSE", "GRAY" }, { "HORSE", "BROWN" } };

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
		assertEquals("Search for null in array without null", -1, ArrayUtilities.indexOf(_itemsMixcase, null));		
	}	
	
	@Test
	public void testSearchArrayWithNullEntryForNull() {
		assertEquals("Search for null in array with null entry", 2, ArrayUtilities.indexOf(_itemsWithNullEntry, null));
	}
	
	@Test
	public void testSearchArrayWithEmptyStringForNullNotFound() {
		assertEquals("Search for null in array with empty strings", -1, ArrayUtilities.indexOf(_itemsWithEmptyStrings, null));		
	}

}
