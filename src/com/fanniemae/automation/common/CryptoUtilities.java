package com.fanniemae.automation.common;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
*
* @author Richard Monson
* @since 2015-12-21
* 
*/
public class CryptoUtilities {

	public static String hashValue(String value) {
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
}
