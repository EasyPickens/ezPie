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

public class Oracle extends JdbcProvider {

	public Oracle(SessionManager session) {
		super(session);
		// example url = "ojdbcx.jar";
		_urlRegex = "(?i).*ojdbc.*\\.jar";
		_className = "oracle.jdbc.driver.OracleDriver";
		_port = 1521;
	}

}
