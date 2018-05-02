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
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2017-09-20
 * 
 */

public class Generic extends JdbcProvider {

	public Generic(SessionManager session) {
		super(session);
		// Used for unknown JDBC provider, user must define URL and class name on connection element.
		_urlRegex = null;
		_className = null;
		_port = 0;
	}

}
