/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.w3c.dom.Element;

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
			throw new RuntimeException(String.format("Error running SQL Scalar command (%s).\n%s\n %s", sqlCommand, sb.toString(), e.getMessage()));
		}
		return result;
	}

	public static DataTable executeCommand(SessionManager _session, Element element) {
		int rowCount = 0;
		DataTable dt = null;
		String sqlCommand = _session.getAttribute(element, "Command");
		if (StringUtilities.isNullOrEmpty(sqlCommand)) {
			throw new RuntimeException(String.format("Missing a value for Command on the %s element.", element.getNodeName()));
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

}
