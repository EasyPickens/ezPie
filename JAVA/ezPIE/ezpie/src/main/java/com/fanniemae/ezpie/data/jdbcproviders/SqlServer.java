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

import com.fanniemae.ezpie.SessionManager;

/**
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2017-09-13
 * 
 */

public class SqlServer extends JdbcProvider {

	public SqlServer(SessionManager session) {
		super(session);
		// example url = "sqljdbc4.jar";
		_urlRegex = "(?i).*sqljdbc.*\\.jar";
		_className = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
		_port = 1433;
	}

}
