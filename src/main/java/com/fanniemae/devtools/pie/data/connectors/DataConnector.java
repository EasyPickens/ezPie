package com.fanniemae.devtools.pie.data.connectors;

import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.StringUtilities;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-22
 * 
 */
public abstract class DataConnector implements AutoCloseable {
	protected SessionManager _session;

	protected int _rowCount = 0;
	protected int _rowLimit = -1; // -1 = no limit

	protected Boolean _schemaOnly = false;
	
	protected Element _connection;
	protected Element _dataSource;

	protected String _dataSourceID;
	protected String _dataSourceType;
	protected String _connectionID;
	protected String _connectionType;
	protected String _connectionDialect;
	protected String _connectionString;

	protected String[][] _dataSchema = new String[][] {};

	public DataConnector(SessionManager session, Element dataSource, Boolean isSchemaOnly) {
		_session = session;
		_dataSource = dataSource;
		_dataSourceID = _dataSource.getAttribute("ID");
		_dataSourceType = _dataSource.getAttribute("Type");
		_schemaOnly = isSchemaOnly;
		
		_connectionID = _dataSource.getAttribute("ConnectionID");
		_connection = _session.getConnection(_connectionID);
		if (_connection != null) {
			_connectionType = _session.getAttribute(_connection, "Type");
			_connectionDialect = _session.getAttribute(_connection, "Dialect");
			_connectionString = _session.getAttribute(_connection, "ConnectionString");
		}
		
		if (StringUtilities.isNullOrEmpty(_dataSourceID))
			throw new RuntimeException(String.format("DataSource.%s is missing an ID value.", _dataSourceType));

		_session.addLogMessage(String.format("DataSource.%s", _dataSourceType), "ID", _dataSourceID);
		
		String sRowLimit = _session.getAttribute(_dataSource, "RowLimit");
		_rowLimit = (StringUtilities.isNullOrEmpty(sRowLimit)) ? -1 : StringUtilities.toInteger(sRowLimit, -1);
		if (_rowLimit != -1) 
			_session.addLogMessage("", "Row Limit", String.format("%,d", _rowLimit));
	}

	public abstract Boolean open();

	public abstract Boolean eof();

	public abstract Object[] getDataRow();

	public abstract void close();

	public int getRowCount() {
		return _rowCount;
	}

	public int getRowLimit() {
		return _rowLimit;
	}

	public String getConnectionString() {
		return _connectionString;
	}

	public String[][] getDataSourceSchema() {
		return _dataSchema;
	}
	
 
}
