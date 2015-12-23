package com.fanniemae.automation.data.utilities;

import java.sql.DatabaseMetaData;
import java.sql.Types;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-22
 * 
 */
public class SqlParameterInfo {
    protected String _Name = "";
    protected int _DbType = Types.VARCHAR;
    protected String _DbTypeName = "";
    protected int _Direction = DatabaseMetaData.procedureColumnIn;
    protected Object _value = null;
    
    public SqlParameterInfo() {
    	
    }
    
    public SqlParameterInfo(String name, int dbType, String dbTypeName, int direction, Object value) {
        _Name = name;
        _DbType = dbType;
        _DbTypeName = dbTypeName;
        _Direction = direction;
        _value = value;
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
    
    public int getDirection() {
    	return _Direction;
    }
    
    public void setDirection(int value) {
    	_Direction = value;
    }
    
    public Object getValue() {
    	return _value;
    }
    
    public void setValue(Object value) {
    	_value = value;
    }
}
