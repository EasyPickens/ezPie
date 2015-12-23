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
	protected DataProvider _Provider;
	protected Connection _con;
	protected PreparedStatement _pstmt;
	protected ResultSet _rs;

	protected String _SqlCommand;

	protected String[] _FieldNames;
	// protected JavaDataType[] _FieldTypes;

	protected Boolean _Closed = true;
	protected Boolean _CalledCommandCancel = false;

	protected int _ColumnCount;
	protected int _CommandTimeout = 60;

	public SqlConnector(SessionManager session, Element dataSource, Boolean isSchemaOnly) {
		super(session, dataSource, isSchemaOnly);

		_SqlCommand = _Session.getAttribute(dataSource, "Command");
	}

	@Override
	public Boolean open() {
		try {
			_Provider = new DataProvider(_Connection);
			_con = _Provider.getConnection();
			_pstmt = _con.prepareStatement(_SqlCommand);
			_ConnectionString = _con.getMetaData().getURL();

			AddCommandParameters();
			// Issue 16358 - Timeout not used in all cases - moved to reader.
			// Issue 17118, 17120 - Set command timeout if any value other than
			// empty string.
			int nCommandTimeout; // = _cmd.getQueryTimeout();
			String sCommandTimeout = _DataSource.getAttribute("CommandTimeOut");
			if (StringUtilities.isNotNullOrEmpty(sCommandTimeout)) {
				sCommandTimeout = _Connection.getAttribute("CommandTimeOut");
			}

			if (StringUtilities.isNotNullOrEmpty(sCommandTimeout) && NotPostgreSQL()) {
				nCommandTimeout = Integer.parseInt(sCommandTimeout);
				_pstmt.setQueryTimeout(nCommandTimeout);
			}

			if (_SchemaOnly) {
				_pstmt.setFetchSize(1);
			} else if (_RowLimit != -1) {
				_pstmt.setFetchSize(_RowLimit);
			}

			_rs = _pstmt.executeQuery();

			ResultSetMetaData rsmd = _rs.getMetaData();
			_ColumnCount = rsmd.getColumnCount();
			_FieldNames = new String[_ColumnCount];
			// _FieldTypes = new JavaDataType[_ColumnCount];
			_DataSchema = new String[_ColumnCount][2];

			String sUsedFieldNames = "";
			int nFieldNumber = 0;
			int nColNumber = 0;
			for (int i = 0; i < _FieldNames.length; i++) {
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
				_FieldNames[nFieldNumber] = sName;
				_DataSchema[nFieldNumber][0] = sName;
				_DataSchema[nFieldNumber][1] = rsmd.getColumnClassName(i + 1);
				nFieldNumber++;
			}
		} catch (NumberFormatException | SQLException ex) {
			throw new RuntimeException("Error while trying to open database connection. " + ex.getMessage(), ex);
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
			Object[] aValues = new Object[_ColumnCount];
			for (int i = 0; i < _ColumnCount; i++) {
				aValues[i] = _rs.getObject(i + 1);
			}
			_RowCount++;
			return aValues;
		} catch (SQLException ex) {
			throw new RuntimeException("Error reading data row values. " + ex.getMessage(), ex);
		}
	}

	protected boolean NotPostgreSQL() throws SQLException {
		String sUrl = _con.getMetaData().getURL().toLowerCase();
		return !sUrl.contains("postgresql");
	}

	protected void AddCommandParameters() throws SQLException {
		// Add parameters in the order listed.
		// Check report definition for defined parameters
		Node nodeParameters = XmlUtilities.selectSingleNode(_DataSource, "SPParameters");
		if (nodeParameters == null) {
			return;
		}
		java.util.Calendar oCalendar;
		String sNullValue = ((Element) nodeParameters).getAttribute("NullValue");

		NodeList nlParameters = XmlUtilities.selectNodes(_DataSource, "SPParameters/SPParameter");

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
		if (_Closed)
			return;

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

		_Closed = true;

	}

}
