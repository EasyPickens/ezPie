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
		Object result = null;
		DataProvider dp = new DataProvider(connection);
		try (Connection con = dp.getConnection()) {
			PreparedStatement pstmt = con.prepareStatement(sqlCommand);
			
			if ((params != null) && (params.length > 0)) {
				for(int i=0;i<params.length;i++) {
					switch (((String)params[i][0]).toLowerCase()) {
					case "int":
						pstmt.setInt(i, (int)params[i][1]);
						break;
					default:
						pstmt.setString(i, params[i][1].toString());
						break;
					}
				}
			}
			pstmt.setFetchSize(1);
			ResultSet rs = pstmt.executeQuery();
			result = rs.getObject(0);
			rs.close();
			pstmt.close();
			con.close();
		} catch (SQLException e) {
			throw new RuntimeException(String.format("Error running SQL Scalar command. %s",e.getMessage()));
		}
		return result;
	}

}
