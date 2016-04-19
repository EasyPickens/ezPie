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
    protected String _name = "";
    protected int _dbType = Types.VARCHAR;
    protected String _dbTypeName = "";
    protected int _direction = DatabaseMetaData.procedureColumnIn;
    protected Object _value = null;
    
    public SqlParameterInfo() {
    	
    }
    
    public SqlParameterInfo(String name, int dbType, String dbTypeName, int direction, Object value) {
        _name = name;
        _dbType = dbType;
        _dbTypeName = dbTypeName;
        _direction = direction;
        _value = value;
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
    
    public int getDirection() {
    	return _direction;
    }
    
    public void setDirection(int value) {
    	_direction = value;
    }
    
    public Object getValue() {
    	return _value;
    }
    
    public void setValue(Object value) {
    	_value = value;
    }
}
