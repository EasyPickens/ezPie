package com.fanniemae.automation.data.utilities;

import java.sql.Types;

/**
 * 
 * @author Richard Monson
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