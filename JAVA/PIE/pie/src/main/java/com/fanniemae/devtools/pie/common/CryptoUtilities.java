/**
 *  
 * Copyright (c) 2015 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPIE/wiki/Fannie-Mae-Open-Source-Licensing-Project
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.devtools.pie.common;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2015-12-21
 * 
 */
public final class CryptoUtilities {

	private CryptoUtilities() {
	}

	public static String hashValue(String value) {
		if (value == null)
			return "";
		else
			return byteArrayToHexString(computeSHA1Hash(value));
	}

	private static byte[] computeSHA1Hash(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			digest.reset();
			digest.update(value.getBytes("UTF-8"));
			return digest.digest();
		} catch (UnsupportedEncodingException | NoSuchAlgorithmException ex) {
			throw new RuntimeException("Error running SHA-1 hash.", ex);
		}
	}

	private static String byteArrayToHexString(byte[] byteArray) {
		String result = "";
		for (int i = 0; i < byteArray.length; i++) {
			result += Integer.toString((byteArray[i] & 0xff) + 0x100, 16).substring(1);
		}
		return result;
	}

	// Simple string encrypting using a single byte salt.
	public static String EncryptDecrypt(String value) {
		byte[] aCrypt = { 0x34 };
		byte[] aInput = value.getBytes();
		int iLength = aInput.length;
		byte[] aOutput = new byte[iLength];

		for (int pos = 0; pos < iLength; ++pos) {
			aOutput[pos] = (byte) (aInput[pos] ^ aCrypt[0]);
		}
		return new String(aOutput);
	}

	// Simple string encrypt/decrypt using a variable length key (or salt).
	public static String EncryptDecrypt(String value, String key) {
		byte[] aCrypt = key.getBytes();
		byte[] aInput = value.getBytes();

		int iLength = aInput.length;
		int iKeyLength = aCrypt.length;

		byte[] aOutput = new byte[iLength];

		int ikeypos = 0;
		for (int pos = 0; pos < iLength; ++pos) {
			aOutput[pos] = (byte) (aInput[pos] ^ aCrypt[ikeypos]);
			ikeypos++;
			if (ikeypos >= iKeyLength) {
				ikeypos = 0;
			}
		}
		return new String(aOutput);
	}
}
