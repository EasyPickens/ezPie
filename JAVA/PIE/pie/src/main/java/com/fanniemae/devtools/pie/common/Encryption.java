package com.fanniemae.devtools.pie.common;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;

import com.fanniemae.devtools.pie.SessionManager;

//@formatter:off
/****
* Copyright 2016 Fannie Mae. Unpublished-rights reserved under the copyright 
* laws of the United States. Use of a copyright notice is a precaution only 
* and does not imply publication or disclosure.
* 
* This software contains confidential information and trade secrets of 
* Fannie Mae. Use, disclosure, or reproduction is prohibited without the prior 
* express written consent of Fannie Mae.
* 
* @author Rick Monson 
* @since 2016-02-19
****/
//@formatter:on
public class Encryption {
	final protected static char[] _hexArray = "0123456789ABCDEF".toCharArray();
	
	protected static boolean _use256 = false;

	// All methods are static, constructor is private to prevent instantiation.
	private Encryption() {
	}

	// Just run this once to enable support for 256 (Only if you don't have rights to JDK/JRE to add JCE file).
	public static void checkEncryptionStrength(SessionManager session) {
		try {
			if ("AIX".equals(System.getProperty("os.name"))) {
				session.addLogMessage("", "AIX Check", "AIX operating system detected, skipping JAVA AES encryption check.");
				return;
			}
			if (javax.crypto.Cipher.getMaxAllowedKeyLength("AES") > 128) {
				// System is already unlocked.
				return;
			}
			String version = System.getProperty("java.version");
			if (version.charAt(2) < '7') {
				session.addLogMessage("** WARNING **", "JCE Policy", "JAVA versions earlier than 1.7 require the JCE policy file to support AES-256 encryption.  Switching to AES-128.");
				return;
			}
			Field field = Class.forName("javax.crypto.JceSecurity").getDeclaredField("isRestricted");
			field.setAccessible(true);
			field.set(null, java.lang.Boolean.FALSE);
			session.addLogMessage("", "Encryption", "Unlimited strength encryption supported.");
			_use256 = true;
		} catch (Exception ex) {
			session.addLogMessage("** WARNING **", "", "Error while trying to enable unlimited strength encryption.");
			session.addErrorMessage(ex);
		}
	}

	public static byte[] encryptData(String data, byte[] key, byte[] salt) {
		return encryptData("AES", data, key, salt);
	}

	public static byte[] encryptData(String method, String data, byte[] key, byte[] salt) {
		if (data == null) {
			return null;
		} else if (data.length() == 0) {
			return new byte[0];
		}

		if (StringUtilities.isNullOrEmpty(method)) {
			method = "AES";
		}
		return toggleEncryption(Cipher.ENCRYPT_MODE, method, key, data.getBytes(), salt);
	}

	public static String decryptData(byte[] data, byte[] key, byte[] salt) {
		return decryptData("AES", data, key, salt);
	}

	public static String decryptData(String method, byte[] data, byte[] key, byte[] salt) {
		if (data == null) {
			return null;
		}
		if (data.length == 0) {
			return "";
		}
		if (StringUtilities.isNullOrEmpty(method)) {
			method = "AES";
		}

		return new String(toggleEncryption(Cipher.DECRYPT_MODE, method, key, data, salt));
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

	public static byte[] toByteArray(String hexString) {
		//return DatatypeConverter.parseHexBinary(hexString);

		int len = hexString.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4) + Character.digit(hexString.charAt(i + 1), 16));
		}
		return data;
	}

	public static byte[] getRandomKey(byte[] salt) {
		return getRandomKey("AES", salt);
	}

	public static byte[] getRandomKey(String method, byte[] salt) {
		KeyGenerator keyGenerator;
		try {
			if (StringUtilities.isNullOrEmpty(method)) {
				method = "AES";
			}
			keyGenerator = KeyGenerator.getInstance(method);
			// Comment out the follow line to switch back to 128 bit AES
			if (_use256 && ((method.equals("AES"))))
				keyGenerator.init(256);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("No provider found for " + method, e);
		}
		byte[] saltedKey = toggleSalt(keyGenerator.generateKey().getEncoded(), salt);
		return saltedKey;
	}

	public static int getKeyLength() {
		return getKeyLength("AES");
	}

	public static int getKeyLength(String method) {
		KeyGenerator keyGenerator;
		try {
			if (StringUtilities.isNullOrEmpty(method)) {
				method = "AES";
			}
			keyGenerator = KeyGenerator.getInstance(method);
			// Comment out the follow line to switch back to 128 bit AES
			if (_use256 && ((method.equals("AES"))))
				keyGenerator.init(256);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("No provider found for " + method, e);
		}
		return keyGenerator.generateKey().getEncoded().length;
	}

	public static byte[] getRandomSalt() {
		return UUID.randomUUID().toString().replace("-", "").getBytes();
	}

	public static String getMD5Hash(String value) {
		return getHash("MD5", value);
	}

	public static String getSHA1Hash(String value) {
		return getHash("SHA-1", value);
	}

	public static String getSHA256Hash(String value) {
		return getHash("SHA-256", value);
	}

	public static byte[] getCertKey(byte[] salt, String appCode, String envCode) {
		String baseKey = null;
		try {
			baseKey = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			// use a default key - need something.
			baseKey = "9AJ378R207GLKJASDGOIJSADGOIASGWERYWNR7WERNY27W52E33";
		}
		int requiredLength = getKeyLength();
		String certKey = getSHA256Hash(String.format("%s%s%s", appCode, baseKey, envCode));
		if (certKey.length() < requiredLength) {
			StringBuilder sb = new StringBuilder(baseKey);
			while (sb.length() < requiredLength) {
				sb.append(certKey);
			}
			certKey = sb.toString();
		}
		if (certKey.length() > requiredLength) {
			certKey = certKey.substring(0, requiredLength);
			byte[] key = certKey.getBytes();
			return toggleSalt(key, salt);
		}
		return null;
	}

	public static byte[] getCertSalt() {
		String salt = null;
		try {
			salt = InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			salt = "LJSADLFKJASD09FJ2035920350275027352037650273";
		}

		return getSHA256Hash(String.format("FE88%s7EF52", salt)).getBytes();
	}

	private static byte[] toggleSalt(byte[] value, byte[] key) {
		if ((value == null) || (value.length == 0)) {
			throw new RuntimeException("Salt value is null or empty.");
		}

		if ((key == null) || (key.length == 0)) {
			throw new RuntimeException("Salt key is null or empty.");
		}

		byte[] workValue = value;

		int length = workValue.length;
		int keyLength = key.length;

		byte[] salted = new byte[length];

		int keyPos = 0;
		for (int pos = 0; pos < length; ++pos) {
			salted[pos] = (byte) (workValue[pos] ^ key[keyPos]);
			keyPos++;
			if (keyPos >= keyLength) {
				keyPos = 0;
			}
		}
		return salted;
	}

	private static byte[] toggleEncryption(int mode, String method, byte[] key, byte[] data, byte[] salt) {
		try {
			byte[] decryptedKey = toggleSalt(key, salt);

			Key secretKey = new SecretKeySpec(decryptedKey, method);
			Cipher cipher = Cipher.getInstance(method);

			cipher.init(mode, secretKey);
			return cipher.doFinal(data);

		} catch (InvalidKeyException exKey) {
			String direction = (mode == Cipher.ENCRYPT_MODE) ? "encryption" : "decryption";
			throw new RuntimeException(String.format("Missing the Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy File. Error while trying to do %s %s of data. \n", method, direction), exKey);
		} catch (Exception ex) {
			String direction = (mode == Cipher.ENCRYPT_MODE) ? "encryption" : "decryption";
			throw new RuntimeException(String.format("Error while trying to do %s %s of data.\n", method, direction), ex);
		}
	}

	private static String getHash(String hashAlgorithm, String value) {
		return (value == null) ? "" : toHexString(computeHash(hashAlgorithm, value));
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
	
}
