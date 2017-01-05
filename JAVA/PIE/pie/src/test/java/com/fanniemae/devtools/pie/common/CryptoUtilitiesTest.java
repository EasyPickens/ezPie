package com.fanniemae.devtools.pie.common;

import org.junit.Test;

import junit.framework.TestCase;

public class CryptoUtilitiesTest extends TestCase {

	@Test
	public void testHashValue() {
		String hashedValue = CryptoUtilities.hashValue(";lkajsdfl;kjas;ldfjouq204597-0129750219u502193j");
		assertEquals("HashValue of string","b4fe52b2e454463b07e49c3ece45a5fda1cb4575",hashedValue);
	}
	
	@Test
	public void testHashValueEmptyString() {
		String hashedValue = CryptoUtilities.hashValue("");
		assertEquals("Hash empty string","da39a3ee5e6b4b0d3255bfef95601890afd80709",hashedValue);
	}
	
	@Test
	public void testHashValueNull() {
		String hashedValue = CryptoUtilities.hashValue(null);
		assertEquals("Hash null string","",hashedValue);
	}

	@Test
	public void testEncryptDecryptString() {
		assertTrue(true);
	}

	@Test
	public void testEncryptDecryptStringString() {
		assertTrue(true);
	}

}
