package com.fanniemae.devtools.pie.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.regex.Pattern;

import org.w3c.dom.Element;
//import org.w3c.dom.NodeList;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.data.DataEngine;
import com.fanniemae.devtools.pie.data.DataProvider;
//import com.fanniemae.devtools.pie.data.connectors.SqlConnector;

public class SqlUtilities {

	private SqlUtilities() {
	}

	public static Object ExecuteScalar(Element connection, String sqlCommand, Object[][] params) {
		return ExecuteScalar(connection, sqlCommand, params, true);
	}

	public static Object ExecuteScalar(Element connection, String sqlCommand, Object[][] params, boolean updateScanManager) {
		if (!updateScanManager)
			return "";

		Object result = null;
		DataProvider dp = new DataProvider(connection);
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

	protected DataStream executeCommand(SessionManager _session, Element element) {
		DataEngine de = new DataEngine(_session);
		DataStream ds = de.getData(element);
		return ds;
		// DataStream ds = null;
		// String command = _session.getAttribute(element, "Command");
		// if (StringUtilities.isNullOrEmpty(command))
		// throw new RuntimeException(String.format("Missing a value for Command on the %s element.", element.getNodeName()));
		//
		// String id = _session.getAttribute(element, "ID");
		// if (StringUtilities.isNullOrEmpty(id))
		// element.setAttribute("ID", "ExcludeDBResultSet");
		//
		// try (SqlConnector conn = new SqlConnector(_session, element, false)) {
		// conn.open();
		//
		// ds = new DataStream();
		// while (!conn.eof()) {
		// datatable.put(datatable.size(), conn.getDataRow());
		// }
		//
		// String[][] schema = conn.getDataSourceSchema();
		// String col = _session.getAttribute(element, "Column");
		// int colIndex = -1;
		// for (int i = 0; i < schema.length; i++) {
		// if (col.equals(schema[i][0])) {
		// colIndex = i;
		// break;
		// }
		// }
		// if (colIndex == -1) {
		// throw new RuntimeException(String.format("Data set returned does not contian a field named %s.", col));
		// }
		//
		// while (!conn.eof()) {
		// Object[] aValues = conn.getDataRow();
		// String value = escapeSpecialChar(aValues[colIndex].toString());
		// excludes.add(Pattern.compile(value));
		// }
		//
		// conn.close();
		// }
		//
		// return excludes;
	}

}
