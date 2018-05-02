/**
 *  
 * Copyright (c) 2017 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.data.jdbcproviders;

import java.io.File;
import java.io.FilenameFilter;

import org.apache.commons.io.filefilter.RegexFileFilter;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.PieException;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2017-09-13
 * 
 */

public abstract class JdbcProvider {

	protected SessionManager _session;

	protected String _urlRegex;
	protected String _className;
	protected int _port;

	public JdbcProvider(SessionManager session) {
		_session = session;
	}

	public String getUrlRegex() {
		return _urlRegex;
	}

	public String getClassName(String className) {
		if ((className != null) && !className.isEmpty()) {
			return className;
		}
		return _className;
	}

	public int getPort(int port) {
		if (port > 0) {
			return port;
		}
		return _port;
	}

	public String getJarFilename(String jdbcJarFilename) {
		if ((jdbcJarFilename != null) && !jdbcJarFilename.isEmpty()) {
			return jdbcJarFilename;
		}
		
		File dir = new File(_session.getJarPath());
		FilenameFilter filter = new RegexFileFilter(_urlRegex);
		File[] files = dir.listFiles(filter);
		if ((files == null) || (files.length == 0)) {
			throw new PieException(String.format("No matching JDBC driver found in %s.", dir.getAbsolutePath()));
		}
		
		File latestDriver = files[0];
		for (int i = 0; i < files.length; i++) {
			if (latestDriver.lastModified() < files[i].lastModified()) {
				latestDriver = files[i];
			}
		}

		return latestDriver.getAbsolutePath();
	}

}
