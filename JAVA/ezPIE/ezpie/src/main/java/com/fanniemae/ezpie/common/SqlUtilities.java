/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.common;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.data.DataProvider;
import com.fanniemae.ezpie.data.connectors.SqlConnector;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-07-08
 * 
 */

public final class SqlUtilities {

	private SqlUtilities() {
	}

	public static Object ExecuteScalar(SessionManager session, Element connection, String sqlCommand, Object[][] params) {
		return ExecuteScalar(session, connection, sqlCommand, params, true);
	}

	public static Object ExecuteScalar(SessionManager session, Element connection, String sqlCommand, Object[][] params, boolean updateScanManager) {
		if (!updateScanManager)
			return "";

		Object result = null;
		DataProvider dp = new DataProvider(session, connection);
		try (Connection con = dp.getConnection()) {
			PreparedStatement pstmt = con.prepareStatement(sqlCommand);

			if ((params != null) && (params.length > 0)) {
				for (int i = 0; i < params.length; i++) {
					Object obj = params[i][1];
					switch (((String) params[i][0]).toLowerCase()) {
					case "int":
						pstmt.setInt(i + 1, (int) obj);
						break;
					default:
						pstmt.setString(i + 1, (String) obj);
						break;
					}
				}
			}
			pstmt.setFetchSize(1);
			Boolean hasResultSet = pstmt.execute();
			if (hasResultSet) {
				ResultSet rs = pstmt.getResultSet();
				rs.next();
				result = rs.getObject(1);
				rs.close();
			} else {
				result = pstmt.getUpdateCount();
			}
			pstmt.close();
			con.close();
		} catch (SQLException e) {
			StringBuilder sb = new StringBuilder();
			sb.append(String.format("SQL Command: %s\n", sqlCommand));
			sb.append(String.format("Is params array null? %s\n", (params == null)));
			if (params != null) {
				sb.append("Value of params array null\n");
				for (int row = 0; row < params.length; row++) {
					for (int col = 0; col < params[row].length; col++) {
						sb.append(String.format("params[%d][%d] => %s\n", row, col, params[row][col]));
					}
				}
			}
			throw new PieException(String.format("Error running SQL Scalar command (%s).\n%s\n %s", sqlCommand, sb.toString(), e.getMessage()), e);
		}
		return result;
	}

	public static DataTable executeCommand(SessionManager _session, Element element) {
		int rowCount = 0;
		DataTable dt = null;
		String sqlCommand = _session.getAttribute(element, "Command");
		if (StringUtilities.isNullOrEmpty(sqlCommand)) {
			throw new PieException(String.format("Missing a value for Command on the %s element.", element.getNodeName()));
		}
		element.setAttribute("Name", "TempName");

		try (SqlConnector conn = new SqlConnector(_session, element, false)) {
			conn.open();
			dt = new DataTable(conn.getDataSourceSchema());
			while (!conn.eof()) {
				dt.addRow(conn.getDataRow());
				rowCount++;
			}
			conn.close();
		}
		_session.addLogMessage("", "Data Returned", String.format("%,d rows, %,d columns", rowCount, dt.getColumnCount()));
		return dt;
	}

	public static void addSqlParameters(SessionManager session, PreparedStatement pstmt, NodeList parameterList) throws SQLException {
		addSqlParameters(session, pstmt, parameterList, false);
	}

	public static void addSqlParameters(SessionManager session, PreparedStatement pstmt, NodeList parameterList, boolean silent) throws SQLException {
		// Add parameters in the order listed.
		int length = parameterList.getLength();
		if (length == 0) {
			return;
		}

		java.util.Date javaDate;
		for (int i = 0; i < length; i++) {
			int paramNumber = i + 1;
			Element eleParameter = (Element) parameterList.item(i);
			String value = session.getAttribute(eleParameter, "Value");
			String paramType = session.getAttribute(eleParameter, "SqlType").trim();
			String nullValue = session.getAttribute(eleParameter, "NullValue");
			if (value.equals(nullValue)) {
				pstmt.setNull(paramNumber, DataUtilities.dbStringTypeToJavaSqlType(paramType));
			}

			String typeUsed = "";
			switch (DataUtilities.dbStringTypeToJavaSqlType(paramType)) {
			case Types.BIGINT:
				pstmt.setLong(paramNumber, Long.parseLong(value));
				typeUsed = "bigint";
				break;
			case Types.BOOLEAN:
				pstmt.setBoolean(paramNumber, Boolean.parseBoolean(value));
				typeUsed = "boolean";
				break;
			case Types.DECIMAL:
				pstmt.setBigDecimal(paramNumber, new BigDecimal(value));
				typeUsed = "decimal";
				break;
			case Types.DATE:
				javaDate = StringUtilities.toDate(value);
				pstmt.setDate(paramNumber, new java.sql.Date(javaDate.getTime()));
				typeUsed = "date";
				break;
			case Types.DOUBLE:
				pstmt.setDouble(paramNumber, Double.parseDouble(value));
				typeUsed = "double";
				break;
			case Types.INTEGER:
				pstmt.setInt(paramNumber, Integer.parseInt(value));
				typeUsed = "integer";
				break;
			case Types.TIME:
				javaDate = StringUtilities.toDate(value);
				pstmt.setTime(paramNumber, new java.sql.Time(javaDate.getTime()));
				typeUsed = "time";
				break;
			case Types.TIMESTAMP:
				javaDate = StringUtilities.toDate(value);
				pstmt.setTimestamp(paramNumber, new java.sql.Timestamp(javaDate.getTime()));
				typeUsed = "timestamp";
				break;
			case Types.CHAR:
			case Types.LONGNVARCHAR:
			case Types.LONGVARCHAR:
			case Types.NVARCHAR:
			case Types.VARCHAR:
				pstmt.setString(paramNumber, value);
				typeUsed = "string";
				break;
			default:
				pstmt.setString(paramNumber, value);
				typeUsed = "string";
				break;
			}

			if (!silent) {
				session.addLogMessage("", "SQL Parameter", String.format("Parameter #%d is set to %s (%s)", paramNumber, value, typeUsed));
			}
		}
	}

}
