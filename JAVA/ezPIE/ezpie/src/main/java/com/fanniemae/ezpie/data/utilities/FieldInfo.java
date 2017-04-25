/**
 *  
 * Copyright (c) 2015 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.data.utilities;

import java.sql.Types;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2015-12-22
 * 
 */

public class FieldInfo {

	protected String _name = "";
	protected int _dbType = Types.VARCHAR;
	protected String _dbTypeName = "VARCHAR";

	public FieldInfo() {
		
	}
	
	public FieldInfo(String name, int dbType, String dbTypeName) {
		_name = name;
		_dbType = dbType;
		_dbTypeName = dbTypeName;
	}
	
	public String getName() {
		return _name;
	}
	
	public void setName(String value) {
		_name = value;
	}

	public int getDbType() {
		return _dbType;
	}
	
	public void setDbType(int value) {
		_dbType = value; 
	}

	public String getDbTypeName() {
		return _dbTypeName;
	}
	
	public void setDbTypeName(String value) {
		_dbTypeName = value;
	}
}