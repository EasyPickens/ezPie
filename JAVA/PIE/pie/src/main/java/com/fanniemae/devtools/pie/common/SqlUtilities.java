package com.fanniemae.devtools.pie.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.data.DataProvider;

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
			throw new RuntimeException(String.format("Error running SQL Scalar command. %s", e.getMessage()));
		}
		return result;
	}

}
