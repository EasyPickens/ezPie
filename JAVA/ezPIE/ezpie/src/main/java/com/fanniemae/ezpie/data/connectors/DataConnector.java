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

package com.fanniemae.ezpie.data.connectors;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.StringUtilities;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
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

	protected String _dataSourceName;
	protected String _dataSourceType;
	protected String _connectionName;
	protected String _connectionType;
	protected String _connectionDialect;
	protected String _connectionString;

	protected String[][] _dataSchema = new String[][] {};

	public DataConnector(SessionManager session, Element dataSource, Boolean isSchemaOnly) {
		_session = session;
		_dataSource = dataSource;
		_dataSourceName = _dataSource.getAttribute("Name");
		_dataSourceType = _dataSource.getAttribute("Type");
		_schemaOnly = isSchemaOnly;

		_connectionName = _dataSource.getAttribute("ConnectionName");
		_connection = _session.getConnection(_connectionName);
		if (_connection != null) {
			_connectionType = _session.getAttribute(_connection, "Type");
			_connectionDialect = _session.getAttribute(_connection, "Dialect");
			_connectionString = _session.getAttribute(_connection, "ConnectionString");
		}

		if ("ExecuteSql".equals(dataSource.getNodeName())) {
			_rowLimit = 1;
			return;
		}

		if (StringUtilities.isNullOrEmpty(_dataSourceName)) {
			_dataSourceName = "- Not Defined -";
		} else {
			_session.addLogMessage(String.format("DataSource.%s", _dataSourceType), "Name", _dataSourceName);
		}

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
