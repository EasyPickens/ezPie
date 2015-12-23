package com.fanniemae.automation.data.utilities;

import java.sql.Types;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-22
 * 
 */
public class FieldInfo {

	protected String _Name = "";
	protected int _DbType = Types.VARCHAR;
	protected String _DbTypeName = "VARCHAR";

	public FieldInfo() {
		
	}
	
	public FieldInfo(String name, int dbType, String dbTypeName) {
		_Name = name;
		_DbType = dbType;
		_DbTypeName = dbTypeName;
	}
	
	public String getName() {
		return _Name;
	}
	
	public void setName(String value) {
		_Name = value;
	}

	public int getDbType() {
		return _DbType;
	}
	
	public void setDbType(int value) {
		_DbType = value; 
	}

	public String getDbTypeName() {
		return _DbTypeName;
	}
	
	public void setDbTypeName(String value) {
		_DbTypeName = value;
	}
}