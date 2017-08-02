/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.common;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-06-15
 * 
 */

final public class Encryption {
	final protected static char[] _hexArray = "0123456789ABCDEF".toCharArray();

	private Encryption() {
	}

	public static String decryptToString(byte[] data, byte[][] key) {
		return decryptToString("AES", data, key);
	}

	public static String decryptToString(String data, byte[][] key) {
		return decryptToString("AES", hexStringToByteArray(data), key);
	}

	public static byte[] encryptToByteArray(String data, byte[][] key) {
		return encryptToByteArray("AES", data, key);
	}

	public static String encryptToString(String data, byte[][] key) {
		if ((data == null) || data.isEmpty()) {
			return "";
		}
		return toHexString(encryptToByteArray("AES", data, key));
	}

	public static int getKeyLength() {
		return getKeyLength("AES");
	}

	public static int getKeyLength(String method) {
		KeyGenerator keyGenerator;
		try {
			if ((method == null) || method.isEmpty()) {
				method = "AES";
			}
			keyGenerator = KeyGenerator.getInstance(method);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("No provider found for " + method, e);
		}
		return keyGenerator.generateKey().getEncoded().length;
	}

	
	public static String getMD5(String value) {
		return getHash("MD5", value);
	}

	public static String getSHA1(String value) {
		return getHash("SHA-1", value);
	}

	public static String getSHA256(String value) {
		return getHash("SHA-256", value);
	}
	
	public static byte[][] setupKey(String encryptionKey) {
		if (encryptionKey == null) {
			throw new RuntimeException("Encryption key is not defined.");
		} else if (encryptionKey.length() < 32) {
			throw new RuntimeException("Encryption key must be at least 32 characters.");
		}
		byte[][] key = new byte[2][];
		key[1] = encryptionKey.substring(17).getBytes();
		key[0] = toggleSalt(encryptionKey.substring(0, 16).getBytes(),key[1]);
		return key;
	}

	public static String toHexString(byte[] byteArray) {
		// Used JavaX which does not work well with WebLogic servers.
		//return DatatypeConverter.printHexBinary(byteArray);

		// Added for WebLogic servers
		if ((byteArray == null) || (byteArray.length == 0)) {
			throw new RuntimeException("Cannot convert a null byte array to a hex string.");
		}
		char[] hexChars = new char[byteArray.length * 2];
		for (int j = 0; j < byteArray.length; j++) {
			int v = byteArray[j] & 0xFF;
			hexChars[j * 2] = _hexArray[v >>> 4];
			hexChars[j * 2 + 1] = _hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
	
	private static byte[] computeHash(String hashAlgorithm, String value) {
		if (value == null) {
			throw new RuntimeException("Cannot compute the hash of a null value.");
		}
		try {
			MessageDigest digest = MessageDigest.getInstance(hashAlgorithm);
			digest.reset();
			digest.update(value.getBytes("UTF-8"));
			return digest.digest();
		} catch (NoSuchAlgorithmException ex) {
			throw new RuntimeException(String.format("Error running %s hash. No such algorithm.", hashAlgorithm), ex);
		} catch (UnsupportedEncodingException ex) {
			throw new RuntimeException(String.format("Error running %s hash. Unsupported encoding exception.", hashAlgorithm), ex);
		}
	}

	private static String decryptToString(String method, byte[] data, byte[][] key) {
		if (data == null) {
			return null;
		}
		if (data.length == 0) {
			return "";
		}
		if (method == null) {
			method = "AES";
		}

		return new String(toggleEncryption(Cipher.DECRYPT_MODE, method, key[0], data, key[1]));
	}

	private static byte[] encryptToByteArray(String method, String data, byte[][] key) {
		if (data == null) {
			return null;
		} else if (data.length() == 0) {
			return new byte[0];
		}

		if (method == null) {
			method = "AES";
		}
		return toggleEncryption(Cipher.ENCRYPT_MODE, method, key[0], data.getBytes(), key[1]);
	}

	private static String getHash(String hashAlgorithm, String value) {
		return (value == null) ? "" : toHexString(computeHash(hashAlgorithm, value));
	}

	private static byte[] hexStringToByteArray(String hexString) {
		// JavaX version below does not work well with WebLogic servers.
		// return DatatypeConverter.parseHexBinary(value);

		int len = hexString.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4) + Character.digit(hexString.charAt(i + 1), 16));
		}
		return data;
	}
	
	private static byte[] toggleEncryption(int mode, String method, byte[] key, byte[] data, byte[] salt) {
		try {
			Key secretKey = new SecretKeySpec(key, method);
			Cipher cipher = Cipher.getInstance(method);

			cipher.init(mode, secretKey);
			return cipher.doFinal(data);

		} catch (InvalidKeyException exKey) {
			String direction = (mode == Cipher.ENCRYPT_MODE) ? "encryption" : "decryption";
			throw new RuntimeException(String.format("Invalid key. Error while trying to do %s %s of data.", method, direction), exKey);
		} catch (Exception ex) {
			String direction = (mode == Cipher.ENCRYPT_MODE) ? "encryption" : "decryption";
			throw new RuntimeException(String.format("Error while trying to do %s %s of data.", method, direction), ex);
		}
	}
	
	private static byte[] toggleSalt(byte[] value, byte[] salt) {
		if ((value == null) || (value.length == 0)) {
			throw new RuntimeException("Value is null or empty.");
		}

		if ((salt == null) || (salt.length == 0)) {
			throw new RuntimeException("Salt is null or empty.");
		}

		byte[] workValue = value;

		int length = workValue.length;
		int keyLength = salt.length;

		byte[] salted = new byte[length];

		int keyPos = 0;
		for (int pos = 0; pos < length; ++pos) {
			salted[pos] = (byte) (workValue[pos] ^ salt[keyPos]);
			keyPos++;
			if (keyPos >= keyLength) {
				keyPos = 0;
			}
		}
		return salted;
	}
	
}
