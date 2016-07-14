package com.fanniemae.devtools.pie.common;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class Miscellaneous {

	private Miscellaneous() {
	}

	public static String getApplicationRoot() {
		String path = getJarLocation();
		if ((path != null) && path.toLowerCase().endsWith("_bin")) {
			return path.substring(0, path.toLowerCase().lastIndexOf("_bin"));
		}
		return path;
	}
	
	public static String getJarLocation() {
		try {
			String path = Miscellaneous.class.getProtectionDomain().getCodeSource().getLocation().getPath();
			String decodedPath = URLDecoder.decode(path, "UTF-8");
			if ((decodedPath != null) && decodedPath.startsWith("/")) {
				decodedPath = decodedPath.substring(1);
			}
			File fi = new File(decodedPath);
			return fi.getParentFile().getPath();
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	public static void sleep(int seconds) {
		try {
			Thread.sleep(seconds * 1000);
		} catch (Exception ex) {
		}
	}
}
