/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.common;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2016-07-12
 * 
 */

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
			ExceptionUtilities.goSilent(e);
			return null;
		}
	}

	public static void sleep(int seconds) {
		try {
			Thread.sleep(seconds * 1000);
		} catch (Exception ex) {
			throw new PieException("Thread sleep interval interrupted. "+ex.getMessage(), ex);
		}
	}
}
