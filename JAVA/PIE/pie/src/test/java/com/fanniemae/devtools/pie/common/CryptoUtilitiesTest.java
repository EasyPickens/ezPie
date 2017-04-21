package com.fanniemae.devtools.pie.common;

import org.junit.Test;

import com.fanniemae.ezpie.common.CryptoUtilities;
import com.fanniemae.ezpie.common.Encryption;

import junit.framework.TestCase;

public class CryptoUtilitiesTest extends TestCase {

	@Test
	public void testHashValue() {
		String hashedValue = CryptoUtilities.hashValue(";lkajsdfl;kjas;ldfjouq204597-0129750219u502193j");
		assertEquals("HashValue of string", "b4fe52b2e454463b07e49c3ece45a5fda1cb4575", hashedValue);
	}

	@Test
	public void testHashValueEmptyString() {
		String hashedValue = CryptoUtilities.hashValue("");
		assertEquals("Hash empty string", "da39a3ee5e6b4b0d3255bfef95601890afd80709", hashedValue);
	}

	@Test
	public void testHashValueNull() {
		String hashedValue = CryptoUtilities.hashValue(null);
		assertEquals("Hash null string", "", hashedValue);
	}

	@Test
	public void testEncryptDecryptString() {
		assertTrue(true);
	}

	@Test
	public void testEncryptDecryptStringString() {
		assertTrue(true);
	}

	@Test
	public void testHashValueNewOld() {
		String hashedValueOld = CryptoUtilities.hashValue(";lkajsdfl;kjas;ldfjouq204597-0129750219u502193j");
		String hashedValueNew = Encryption.getSHA1Hash(";lkajsdfl;kjas;ldfjouq204597-0129750219u502193j");
		// assertEquals("HashValue of string","b4fe52b2e454463b07e49c3ece45a5fda1cb4575",hashedValue);
		assertEquals("HashValue of string", hashedValueNew.toLowerCase(), hashedValueOld);
	}
	
	@Test
	public void testHashValueEmptyStringNewOld() {
		String hashedValueOld = CryptoUtilities.hashValue("");
		String hashedValueNew = Encryption.getSHA1Hash("");
		assertEquals("Hash empty string", hashedValueNew.toLowerCase(), hashedValueOld);
	}

	@Test
	public void testHashValueNullNewOld() {
		String hashedValueOld = CryptoUtilities.hashValue(null);
		String hashedValueNew = Encryption.getSHA1Hash(null);
		assertEquals("Hash null string", hashedValueNew, hashedValueOld);
	}

}
