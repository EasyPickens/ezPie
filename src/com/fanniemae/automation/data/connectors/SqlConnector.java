package com.fanniemae.automation.data.connectors;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fanniemae.automation.SessionManager;
import com.fanniemae.automation.common.DataUtilities;
import com.fanniemae.automation.common.FileUtilities;
import com.fanniemae.automation.common.StringUtilities;
import com.fanniemae.automation.common.XmlUtilities;
import com.fanniemae.automation.data.DataProvider;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-22
 * 
 */
public class SqlConnector extends DataConnector {
	protected DataProvider _provider;
	protected Connection _con;
	protected PreparedStatement _pstmt;
	protected ResultSet _rs;

	protected String _sqlCommand;

	protected String[] _fieldNames;

	protected Boolean _calledCommandCancel = false;

	protected int _columnCount;
	protected int _commandTimeout = 60;

	public SqlConnector(SessionManager session, Element dataSource, Boolean isSchemaOnly) {
		super(session, dataSource, isSchemaOnly);

		_sqlCommand = _session.getAttribute(dataSource, "Command");
		if (_sqlCommand.startsWith("file://")) {
			_sqlCommand = _session.resolveTokens(FileUtilities.loadFile(_sqlCommand.substring(7)));
		}
		_session.addLogMessagePreserveLayout("", "Command", _sqlCommand);
	}

	@Override
	public Boolean open() {
		try {
			_provider = new DataProvider(_connection);
			_con = _provider.getConnection();
			_pstmt = _con.prepareStatement(_sqlCommand);
			_connectionString = _con.getMetaData().getURL();

			_session.addLogMessage("", "ConnectionID", _connection.getAttribute("ID"));

			AddCommandParameters();
			String sCommandTimeout = _dataSource.getAttribute("CommandTimeout");
			if (StringUtilities.isNullOrEmpty(sCommandTimeout)) {
				sCommandTimeout = _connection.getAttribute("CommandTimeout");
			}

			if (StringUtilities.isNotNullOrEmpty(sCommandTimeout)) { // &&
																		// NotPostgreSQL())
																		// {
				_pstmt.setQueryTimeout(StringUtilities.toInteger(sCommandTimeout, 60));
				_session.addLogMessage("", "Command Timeout", String.format("%,d", _pstmt.getQueryTimeout()));
				// } else if (StringUtilities.isNotNullOrEmpty(sCommandTimeout)
				// && isPostgreSQL()) {
				// int iTimeout = StringUtilities.toInteger(sCommandTimeout, 60)
				// * 1000;
				// if (!_SqlCommand.trim().endsWith(";"))
				// _SqlCommand = _SqlCommand.trim() + ";";
				// _pstmt =
				// _con.prepareStatement(String.format("SET statement_timeout TO %d; \n%s\n RESET statement_timeout;",
				// iTimeout, _SqlCommand));
				// _Session.addLogMessage("", "Command Timeout",
				// String.format("%,d", iTimeout / 1000));
			}

			if (_schemaOnly) {
				_pstmt.setFetchSize(1);
			} else if (_rowLimit != -1) {
				_pstmt.setFetchSize(_rowLimit);
				_session.addLogMessage("", "Row Limit", String.format("%, d", _rowLimit));
			}

			_session.addLogMessage("", "Execute Query", "Send the query to the database server.");
			// JDBC executeQuery does not support SQL queries that combine
			// multiple statements
			// _rs = _pstmt.executeQuery();
			boolean isResultSet = _pstmt.execute();
			if (isResultSet) {
				_rs = _pstmt.getResultSet();
			} else {
				// Look through up to 30 results, taking only the first ResultSet.
				// Could switch to a while(true), but concerned about infinite loop
				for (int i = 0; i < 31; i++) {
					isResultSet = _pstmt.getMoreResults();
					if (isResultSet) {
						_rs = _pstmt.getResultSet();
						break;
					} else if (_pstmt.getUpdateCount() == -1) {
						break;
					}
				}
			}
			_session.addLogMessage("", "", "Database server returned.");

			if (_rs == null) {
				RuntimeException ex = new RuntimeException("Query returned null result set information.");
				throw ex;
			}
			
			_session.addLogMessage("", "Read MetaData", "Read field names and data types.");
			ResultSetMetaData rsmd = _rs.getMetaData();
			_columnCount = rsmd.getColumnCount();
			_fieldNames = new String[_columnCount];
			// _FieldTypes = new JavaDataType[_ColumnCount];
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
			_session.addLogMessage("", "Query Returned", sbFields.toString());
		} catch (NumberFormatException | SQLException ex) {
			throw new RuntimeException("Error while trying to open and run the query. " + ex.getMessage(), ex);
		}
		return true;
	}

	@Override
	public Boolean eof() {
		try {
			return !_rs.next();
		} catch (SQLException e) {
			return true;
		}
	}

	@Override
	public Object[] getDataRow() {
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

	protected boolean NotPostgreSQL() throws SQLException {
		return !isPostgreSQL();
	}

	protected void AddCommandParameters() throws SQLException {
		// Add parameters in the order listed.
		// Check report definition for defined parameters
		Node nodeParameters = XmlUtilities.selectSingleNode(_dataSource, "SPParameters");
		if (nodeParameters == null) {
			return;
		}
		java.util.Calendar oCalendar;
		String sNullValue = ((Element) nodeParameters).getAttribute("NullValue");

		NodeList nlParameters = XmlUtilities.selectNodes(_dataSource, "SPParameters/SPParameter");

		if ((nlParameters != null) && (nlParameters.getLength() > 0)) {
			int iLength = nlParameters.getLength();
			for (int i = 0; i < iLength; i++) {
				int iParam = i + 1;
				// String sName = ((Element)
				// nlParameters.item(i)).getAttribute("Name").trim();
				String sValue = ((Element) nlParameters.item(i)).getAttribute("Value");
				String sParamType = ((Element) nlParameters.item(i)).getAttribute("SqlParamType").trim();
				if (sValue.equals(sNullValue)) {
					_pstmt.setNull(iParam, DataUtilities.dbStringTypeToJavaSqlType(sParamType));
				}

				switch (DataUtilities.dbStringTypeToJavaSqlType(sParamType)) {
				case Types.BIGINT:
					_pstmt.setLong(iParam, Long.parseLong(sValue));
					break;
				case Types.BOOLEAN:
					_pstmt.setBoolean(iParam, Boolean.parseBoolean(sValue));
					break;
				case Types.DECIMAL:
					_pstmt.setBigDecimal(iParam, new BigDecimal(sValue));
					break;
				case Types.DATE:
					oCalendar = javax.xml.bind.DatatypeConverter.parseDateTime(sValue);
					_pstmt.setDate(iParam, new java.sql.Date(oCalendar.getTimeInMillis()));
					break;
				case Types.DOUBLE:
					_pstmt.setDouble(iParam, Double.parseDouble(sValue));
					break;
				case Types.INTEGER:
					_pstmt.setInt(iParam, Integer.parseInt(sValue));
					break;
				case Types.TIME:
					oCalendar = javax.xml.bind.DatatypeConverter.parseDateTime(sValue);
					_pstmt.setTime(iParam, new java.sql.Time(oCalendar.getTimeInMillis()));
					break;
				case Types.TIMESTAMP:
					oCalendar = javax.xml.bind.DatatypeConverter.parseDateTime(sValue);
					_pstmt.setTimestamp(iParam, new java.sql.Timestamp(oCalendar.getTimeInMillis()));
					break;
				case Types.CHAR:
				case Types.LONGNVARCHAR:
				case Types.LONGVARCHAR:
				case Types.NVARCHAR:
				case Types.VARCHAR:
					_pstmt.setString(iParam, sValue);
					break;
				default:
					_pstmt.setString(iParam, sValue);
					break;
				}

			}
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
		}

		try {
			if (_pstmt != null) {
				_pstmt.close();
				_pstmt = null;
			}
		} catch (SQLException e) {
		}

		try {
			if (_con != null) {
				_con.close();
				_con = null;
			}
		} catch (SQLException e) {
		}
	}

}
