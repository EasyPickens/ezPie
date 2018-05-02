/**
 *  
 * Copyright (c) 2015 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.data.connectors;

import java.io.File;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.Constants;
import com.fanniemae.ezpie.common.DataUtilities;
import com.fanniemae.ezpie.common.ExceptionUtilities;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.common.XmlUtilities;
import com.fanniemae.ezpie.data.DataProvider;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2015-12-22
 * 
 */

public class SqlConnector extends DataConnector {
	protected DataProvider _provider;
	protected Connection _con;
	protected PreparedStatement _pstmt;
	protected CallableStatement _cstmt;
	protected ResultSet _rs;

	protected String _sqlCommand;
	protected String _sqlStoredProcedure;

	protected String[] _fieldNames;

	protected Boolean _isStoredProcedure = false;
	protected Boolean _calledCommandCancel = false;
	protected Boolean _usingTransactions = false;
	protected Boolean _onlyUpdateCount = false;

	protected int _columnCount;
	protected int _commandTimeout = 60;
	protected int _updateCount = -1;

	public SqlConnector(SessionManager session, Element dataSource, Boolean isSchemaOnly) {
		super(session, dataSource, isSchemaOnly);

		if ("SP".equals(dataSource.getAttribute("Type"))) {
			_sqlStoredProcedure = _session.requiredAttribute(dataSource, "StoredProcedure");
			_isStoredProcedure = true;
		} else {
			_sqlCommand = _session.getAttribute(dataSource, "Command");
			if (_sqlCommand.startsWith("file://")) {
				String filename = _sqlCommand.substring(7);
				if (FileUtilities.isInvalidFile(filename)) {
					String resourceDir = String.format("[Configuration.ApplicationPath]%s_Resources%s%s", File.separator, File.separator, filename);
					resourceDir = _session.resolveTokens(resourceDir);
					if (FileUtilities.isValidFile(resourceDir)) {
						filename = resourceDir;
					} else {
						throw new PieException(String.format("SQL command file %s was not found.", filename));
					}
				}
				_sqlCommand = _session.resolveTokens(FileUtilities.loadFile(filename));
			}
			_session.addLogMessagePreserveLayout("", "Command", _sqlCommand);

			// For ExecuteSql elements we use transactions by default
			_usingTransactions = "ExecuteSql".equals(_dataSource.getNodeName());
		}
	}

	@Override
	public Boolean open() {
		try {
			_provider = new DataProvider(_session, _connection);
			_con = _provider.getConnection();
			if (_usingTransactions)
				_con.setAutoCommit(false);

			if (_isStoredProcedure) {
				_pstmt = _con.prepareCall(_sqlStoredProcedure);
			} else {
				_pstmt = _con.prepareStatement(_sqlCommand);
			}
			_connectionString = _con.getMetaData().getURL();

			_session.addLogMessage("", "ConnectionName", _connection.getAttribute("Name"));

			addSqlParameters();
			String sCommandTimeout = _dataSource.getAttribute("CommandTimeout");
			if (StringUtilities.isNullOrEmpty(sCommandTimeout)) {
				sCommandTimeout = _connection.getAttribute("CommandTimeout");
			}

			if (StringUtilities.isNotNullOrEmpty(sCommandTimeout)) { // &&
																		// NotPostgreSQL())
																		// {
				_pstmt.setQueryTimeout(StringUtilities.toInteger(sCommandTimeout, 60));
				_session.addLogMessage("", "Command Timeout", String.format("%,d", _pstmt.getQueryTimeout()));
			}

			if (_schemaOnly) {
				_pstmt.setFetchSize(1);
			} else if (_rowLimit != -1) {
				_pstmt.setFetchSize(_rowLimit);
				// _session.addLogMessage("", "Row Limit", String.format("%, d", _rowLimit));
			}

			_session.addLogMessage("", "Execute Query", "Send the query to the database server.");
			boolean isResultSet = _pstmt.execute();
			if (isResultSet) {
				_rs = _pstmt.getResultSet();
			} else {
				_onlyUpdateCount = true;
				_updateCount = _pstmt.getUpdateCount();
				_fieldNames = new String[] { "rowsaffected" };
				_dataSchema = new String[1][2];
				_dataSchema[0][0] = "rowsaffected";
				_dataSchema[0][1] = "java.lang.Integer";
				_session.addLogMessage("", "Result Set Schema", "rowsaffected (java.lang.Integer)");
			}
			_session.addLogMessage("", "Query Returned", "Database server results ready.");

			if (!_onlyUpdateCount) {
				if (_rs == null) {
					RuntimeException ex = new RuntimeException("Query returned null result set information.");
					throw ex;
				}

				_session.addLogMessage("", "Read MetaData", "Read field names and data types.");
				ResultSetMetaData rsmd = _rs.getMetaData();
				_columnCount = rsmd.getColumnCount();
				_fieldNames = new String[_columnCount];
				_dataSchema = new String[_columnCount][2];

				StringBuilder sbFields = new StringBuilder();
				String sUsedFieldNames = "";
				int nFieldNumber = 0;
				int nColNumber = 0;
				for (int i = 0; i < _fieldNames.length; i++) {
					String sName = rsmd.getColumnName(i + 1);
					if (StringUtilities.isNullOrEmpty(sName)) {
						sName = String.format("Column%s", nColNumber);
						nColNumber++;
					}

					String sTarget = String.format(";%s;", sName).toLowerCase();
					if (sUsedFieldNames.contains(sTarget)) {
						// Duplicate columns must be included with a unique name.
						// The code will add a sequential number to the field name
						// until it is unique - or use GUID.
						for (int x = 2; x < 500; x++) {
							sTarget = String.format(";%s%s;", sName, x).toLowerCase();
							if (!sUsedFieldNames.contains(sTarget)) {
								sName = String.format("%s%s", sName, x);
								break;
							}
						}
					}
					sUsedFieldNames += sTarget;
					_fieldNames[nFieldNumber] = sName;
					_dataSchema[nFieldNumber][0] = sName;
					_dataSchema[nFieldNumber][1] = rsmd.getColumnClassName(i + 1);
					if (i > 0)
						sbFields.append(",\n");
					sbFields.append(String.format("%s (%s)", sName, rsmd.getColumnClassName(i + 1)));
					nFieldNumber++;
				}
				_session.addLogMessage("", "Result Set Schema", sbFields.toString());
			}

			if (_usingTransactions)
				_con.commit();
		} catch (NumberFormatException | SQLException ex) {
			if (_usingTransactions) {
				try {
					Exception exRun = new RuntimeException("ExecuteSql transaction is being rolled back. " + ex.getMessage(), ex);
					_session.addErrorMessage(exRun);
					_con.rollback();
				} catch (SQLException e) {
					throw new RuntimeException("Error during rollback. " + e.getMessage(), e);
				}
			}
			throw new RuntimeException("Error while trying to open and run the query. " + ex.getMessage(), ex);
		}
		return true;
	}

	@Override
	public Boolean eof() {
		if (_onlyUpdateCount) {
			return false;
		} else if (_rs == null) {
			return true;
		}
		try {
			return !_rs.next();
		} catch (SQLException e) {
			ExceptionUtilities.goSilent(e);
			return true;
		}
	}

	@Override
	public Object[] getDataRow() {
		if (_onlyUpdateCount) {
			// After reading the update count, you have reached the end of the result set.
			_onlyUpdateCount = false;
			return new Object[] { _updateCount };
		}
		try {
			Object[] aValues = new Object[_columnCount];
			for (int i = 0; i < _columnCount; i++) {
				aValues[i] = _rs.getObject(i + 1);
			}
			_rowCount++;
			return aValues;
		} catch (SQLException ex) {
			throw new RuntimeException("Error reading data row values. " + ex.getMessage(), ex);
		}
	}

	protected boolean isPostgreSQL() throws SQLException {
		String sUrl = _con.getMetaData().getURL().toLowerCase();
		return sUrl.contains("postgresql");
	}

	protected boolean isNotPostgreSQL() throws SQLException {
		return !isPostgreSQL();
	}

	protected void addSqlParameters() throws SQLException {
		// Add parameters in the order listed.
		// Check report definition for defined parameters
		NodeList parameterList = XmlUtilities.selectNodes(_dataSource, "SqlParameter");
		int length = parameterList.getLength();
		if (length == 0)
			return;

		java.util.Date javaDate;
		for (int i = 0; i < length; i++) {
			int paramNumber = i + 1;
			Element eleParameter = (Element) parameterList.item(i);
			String value = _session.getAttribute(eleParameter, "Value");
			String paramType = _session.getAttribute(eleParameter, "SqlType").trim();
			String nullValue = _session.getAttribute(eleParameter, "NullValue");
			if (value.equals(nullValue)) {
				_pstmt.setNull(paramNumber, DataUtilities.dbStringTypeToJavaSqlType(paramType));
			}

			switch (DataUtilities.dbStringTypeToJavaSqlType(paramType)) {
			case Types.BIGINT:
				_pstmt.setLong(paramNumber, Long.parseLong(value));
				break;
			case Types.BOOLEAN:
				_pstmt.setBoolean(paramNumber, Boolean.parseBoolean(value));
				break;
			case Types.DECIMAL:
				_pstmt.setBigDecimal(paramNumber, new BigDecimal(value));
				break;
			case Types.DATE:
				javaDate = StringUtilities.toDate(value);
				_pstmt.setDate(paramNumber, new java.sql.Date(javaDate.getTime()));
				break;
			case Types.DOUBLE:
				_pstmt.setDouble(paramNumber, Double.parseDouble(value));
				break;
			case Types.INTEGER:
				_pstmt.setInt(paramNumber, Integer.parseInt(value));
				break;
			case Types.TIME:
				javaDate = StringUtilities.toDate(value);
				_pstmt.setTime(paramNumber, new java.sql.Time(javaDate.getTime()));
				break;
			case Types.TIMESTAMP:
				javaDate = StringUtilities.toDate(value);
				_pstmt.setTimestamp(paramNumber, new java.sql.Timestamp(javaDate.getTime()));
				break;
			case Types.CHAR:
			case Types.LONGNVARCHAR:
			case Types.LONGVARCHAR:
			case Types.NVARCHAR:
			case Types.VARCHAR:
				_pstmt.setString(paramNumber, value);
				break;
			default:
				_pstmt.setString(paramNumber, value);
				break;
			}
			_session.addLogMessage("", "SQL Parameter", String.format("Parameter #%d is set to %s", paramNumber, value));
		}
	}

	@Override
	public void close() {
		try {
			if (_rs != null) {
				_rs.close();
				_rs = null;
			}
		} catch (SQLException e) {
			_session.addLogMessage(Constants.LOG_WARNING_MESSAGE, "SQL", "Error while trying to close result set. " + e.getMessage());
			ExceptionUtilities.goSilent(e);
		}

		try {
			if (_pstmt != null) {
				_pstmt.close();
				_pstmt = null;
			}
		} catch (SQLException e) {
			_session.addLogMessage(Constants.LOG_WARNING_MESSAGE, "SQL", "Error while trying to close prepared statement. " + e.getMessage());
			ExceptionUtilities.goSilent(e);
		}

		try {
			if (_con != null) {
				_con.close();
				_con = null;
			}
		} catch (SQLException e) {
			_session.addLogMessage(Constants.LOG_WARNING_MESSAGE, "SQL", "Error while trying to close database connection. " + e.getMessage());
			ExceptionUtilities.goSilent(e);
		}
	}

}
