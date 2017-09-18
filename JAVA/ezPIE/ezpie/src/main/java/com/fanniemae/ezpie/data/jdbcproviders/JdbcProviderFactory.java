/**
 *  
 * Copyright (c) 2017 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.data.jdbcproviders;

import com.fanniemae.ezpie.SessionManager;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2017-09-18
 * 
 */

public class JdbcProviderFactory {
	
	private JdbcProviderFactory() {}
	
	public static JdbcProvider getProvider(SessionManager session, String sqlServerType) {
		switch (sqlServerType.toLowerCase()) {
		case "postgres":
			return new Postgres(session);
		case "sqlserver":
			return new SqlServer(session);
		case "oracle":
			return new Oracle(session);
		default:
			throw new RuntimeException(String.format("No default provider information defined for %s database servers.", sqlServerType));
		}
	}

}
