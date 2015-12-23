package com.fanniemae.automation.data.connectors;

import org.w3c.dom.Element;

import com.fanniemae.automation.SessionManager;
import com.fanniemae.automation.common.StringUtilities;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-22
 * 
 */
public abstract class DataConnector implements AutoCloseable {
	protected SessionManager _Session;

	protected int _RowCount = 0;
	protected int _RowLimit = -1; // -1 = no limit

	protected Boolean _SchemaOnly = false;
	
	protected Element _Connection;
	protected Element _DataSource;

	protected String _DataSourceID;
	protected String _DataSourceType;
	protected String _ConnectionID;
	protected String _ConnectionType;
	protected String _ConnectionDialect;
	protected String _ConnectionString;

	protected String[][] _DataSchema = new String[][] {};

	public DataConnector(SessionManager session, Element dataSource, Boolean isSchemaOnly) {
		_Session = session;
		_DataSource = dataSource;
		_DataSourceID = _DataSource.getAttribute("ID");
		_DataSourceType = _DataSource.getAttribute("Type");
		_SchemaOnly = isSchemaOnly;
		_ConnectionID = _DataSource.getAttribute("ConnectionID");
		_Connection = _Session.getConnection(_ConnectionID);
		if (_Connection != null) {
			_ConnectionType = _Session.getAttribute(_Connection, "Type");
			_ConnectionDialect = _Session.getAttribute(_Connection, "Dialect");
			_ConnectionString = _Session.getAttribute(_Connection, "ConnectionString");
		}
		if (StringUtilities.isNullOrEmpty(_DataSourceID))
			throw new RuntimeException(String.format("DataSource.%s is missing an ID value.", _DataSourceType));

		_Session.addLogMessage(String.format("DataSource.%s", _DataSourceType), "ID", _DataSourceID);
	}

	public abstract Boolean open();

	public abstract Boolean eof();

	public abstract Object[] getDataRow();

	public abstract void close();

	public int getRowCount() {
		return _RowCount;
	}

	public int getRowLimit() {
		return _RowLimit;
	}

	public String getConnectionString() {
		return _ConnectionString;
	}

	public String[][] getDataSourceSchema() {
		return _DataSchema;
	}
	
 
}
