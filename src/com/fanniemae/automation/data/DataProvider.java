package com.fanniemae.automation.data;

import java.net.URL;
import java.net.URLClassLoader;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import com.fanniemae.automation.common.StringUtilities;
import com.fanniemae.automation.data.utilities.DriverShim;
import com.fanniemae.automation.data.utilities.FieldInfo;
import com.fanniemae.automation.data.utilities.SqlParameterInfo;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-22
 * 
 */
public class DataProvider {
	protected Element _Connection = null;

	protected String _SqlDialect = "";
	protected String _ConnectionString = "";
	protected String _LastErrorMessage = "";
	protected String _JarFilename = "";
	protected String _ClassName = "";
	protected String _URL;

	protected int _CommandTimeout = 60;

	public DataProvider(Element eleConnection) {
		_Connection = eleConnection;

		_SqlDialect = eleConnection.getAttribute("Dialect");
		if (StringUtilities.isNullOrEmpty(_SqlDialect)) {
			_SqlDialect = "SQL92";
		}
	}

	public Connection getConnection() {
		Connection con = null;
		try {
			URL u = new URL(formatProviderUrl(_URL));
			URLClassLoader ucl = new URLClassLoader(new URL[] { u });
			Driver d = (Driver) Class.forName(_ClassName, true, ucl).newInstance();
			DriverManager.registerDriver(new DriverShim(d));
			con = DriverManager.getConnection(_ConnectionString);
		} catch (Exception ex) {
			_LastErrorMessage = ex.getMessage();
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
				}
			}
			con = null;
		}
		return con;
	}

	public List<FieldInfo> getSchema(String sSqlCommand) {
		List<FieldInfo> aResults = new ArrayList<>();
		ResultSet rs = null;
		try (Connection con = getConnection(); Statement stmt = con.createStatement()) {
			stmt.setMaxRows(1);
			stmt.setQueryTimeout(_CommandTimeout);
			rs = stmt.executeQuery(sSqlCommand);

			ResultSetMetaData oSchema = rs.getMetaData();
			for (int i = 1; i <= oSchema.getColumnCount(); i++) {
				FieldInfo oInfo = new FieldInfo();
				oInfo.setName(oSchema.getColumnName(i));
				oInfo.setDbType(oSchema.getColumnType(i));
				oInfo.setDbTypeName(oSchema.getColumnTypeName(i));
				aResults.add(oInfo);
			}
		} catch (SQLException ex) {
			_LastErrorMessage = ex.getMessage();
			aResults = null;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
				}
			}
		}
		return aResults;
	}

	public CallableStatement deriveParameters(Connection con, String storedProcName) {
		CallableStatement cstmt = null;
		List<SqlParameterInfo> aParams = new ArrayList<>();
		Boolean bReturnValue = false;

		try {
			DatabaseMetaData dbmd = con.getMetaData();
			try (ResultSet rs = dbmd.getProcedureColumns(null, null, storedProcName, null)) {
				while (rs.next()) {
					if (rs.getInt(5) == DatabaseMetaData.procedureColumnReturn) {
						bReturnValue = true;
					}
					SqlParameterInfo oParam = new SqlParameterInfo();
					oParam.setName(rs.getString(4));
					oParam.setDirection(rs.getInt(5));
					oParam.setDbType(rs.getInt(6));
					oParam.setDbTypeName(rs.getString(7));
					aParams.add(oParam);
				}
			}

			if (aParams.isEmpty() && bReturnValue) {
				cstmt = con.prepareCall("{? = call " + storedProcName + "()}");
			} else if (aParams.isEmpty() && (!bReturnValue)) {
				cstmt = con.prepareCall("{call " + storedProcName + "()}");
			} else {
				int iParamCount = aParams.size();
				StringBuilder sb = new StringBuilder();
				sb.append("{");
				if (bReturnValue) {
					sb.append("? = ");
				}
				sb.append("call ");
				sb.append(storedProcName);
				sb.append("(");
				for (int i = 1; i < iParamCount; i++) {
					if (i > 1) {
						sb.append(", ");
					}
					sb.append("?");
				}
				sb.append(")}");
				cstmt = con.prepareCall(sb.toString());

				int iParam = 1;
				for (SqlParameterInfo oParam : aParams) {
					switch (oParam.getDirection()) {
					case DatabaseMetaData.procedureColumnIn:
						cstmt = addInParameter(cstmt, iParam, oParam.getDbType(), oParam.getDbTypeName());
						break;
					case DatabaseMetaData.procedureColumnInOut:
					case DatabaseMetaData.procedureColumnOut:
					case DatabaseMetaData.procedureColumnResult:
					case DatabaseMetaData.procedureColumnReturn:
						cstmt.registerOutParameter(iParam, oParam.getDbType());
						break;
					}
					iParam++;
				}

			}
		} catch (SQLException e) {
			_LastErrorMessage = e.getMessage();
		}
		return cstmt;
	}

	protected CallableStatement addInParameter(CallableStatement cstmt, int param, int dbType, String dbTypeName) throws SQLException {
		switch (dbType) {
		case Types.BOOLEAN:
			cstmt.setBoolean(param, true);
			break;
		case Types.CHAR:
			cstmt.setString(param, "");
			break;
		case Types.DATE:
			cstmt.setDate(param, null);
			break;
		case Types.DECIMAL:
			// cstmt.setBigDecimal(iParam, BigDecimal.ZERO);
			cstmt.setDouble(param, 0.0);
			break;
		case Types.INTEGER:
			cstmt.setInt(param, 0);
			break;
		case Types.VARCHAR:
			cstmt.setString(param, "");
			break;
		default:
			throw new SQLException("Stored procedure parameter type " + dbTypeName + " is not a currently supported input type");
		}
		return cstmt;
	}

	protected String formatProviderUrl(String sUrl) {
		String sResult = sUrl;

		if (!sUrl.toLowerCase().startsWith("jar:file:")) {
			sResult = String.format("jar:file:%s!/", sUrl);
		}
		return sResult;
	}
}
