package com.fanniemae.ezpie.data.transforms.validation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.DataUtilities;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.common.SqlUtilities;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.common.XmlUtilities;
import com.fanniemae.ezpie.data.DataProvider;

/**
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2018-09-18
 * 
 */

public class ValidateSql extends DataValidation {

	protected Connection _con;

	protected String _sqlCommand;
	protected int _commandTimeout = 60;

	protected NodeList _parameterList;

	public ValidateSql(SessionManager session, Element transform, String[][] inputSchema) {
		super(session, transform, inputSchema);

		_allowNulls = StringUtilities.toBoolean(getOptionalAttribute("AllowNulls"), _allowNulls);

		String sqlConnectionName = getRequiredAttribute("ConnectionName");
		_sqlCommand = getRequiredAttribute("Command");

		Element connection = _session.getConnection(sqlConnectionName);
		try {
			DataProvider provider = new DataProvider(_session, connection);
			_con = provider.getConnection();
			_con.setAutoCommit(false);

			String commandTimeout = getOptionalAttribute("CommandTimeout");
			if (StringUtilities.isNullOrEmpty(commandTimeout)) {
				commandTimeout = _session.optionalAttribute(connection, "CommandTimeout");
			}

			if (StringUtilities.isNotNullOrEmpty(commandTimeout)) {
				_commandTimeout = StringUtilities.toInteger(commandTimeout);
			}
		} catch (SQLException e) {
			throw new PieException("Error while setting up SQL validation connection. " + e.getMessage(), e);
		}

		_parameterList = XmlUtilities.selectNodes(_transform, "SqlParameter");
		int length = _parameterList.getLength();
		for (int i = 0; i < length; i++) {
			_session.addLogMessage("", String.format("SQL Parameter $d", i + 1), ((Element) _parameterList.item(i)).getAttribute("Value"));
		}

	}

	@Override
	public Object[] validateDataRow(Object[] dataRow) {
		_session.setDataTokens(DataUtilities.dataRowToTokenHash(_inputSchema, dataRow));

		Object[] validationResults = new Object[] { _rowNumber, _dataColumn, "null", "SQL validation could not be performed.", new Date() };
		_rowNumber++;

		Object objValue = dataRow[_sourceColumnIndex];
		if ((objValue == null) && !_allowNulls) {
			validationResults[3] = "No value provided.";
			return validationResults;
		} else if (objValue == null) {
			return null;
		}

		boolean isValid = false;
		ResultSet rs = null;
		try (PreparedStatement pstmt = _con.prepareStatement(_sqlCommand);) {
			pstmt.setQueryTimeout(_commandTimeout);
			pstmt.setFetchSize(1);

			SqlUtilities.addSqlParameters(_session, pstmt, _parameterList, true);

			boolean isResultSet = pstmt.execute();
			if (isResultSet) {
				rs = pstmt.getResultSet();
				isValid = rs.next();
			} else {
				int updateCount = pstmt.getUpdateCount();
				isValid = updateCount > 0;
			}
		} catch (SQLException e) {
			throw new PieException("Error while trying to run validation command. " + e.getMessage(), e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException ex) {
					throw new PieException("could not close result set during SQL validation. " + ex.getMessage(), ex);
				}
			}
		}

		if (!isValid) {
			validationResults[3] = "Provided value was not found in the database.";
			validationResults[2] = objValue.toString();
			return validationResults;
		}

		return null;
	}

	@Override
	public void close() {
		if (_con != null) {
			try {
				_con.close();
			} catch (SQLException e) {
				throw new PieException("Error while trying to close the SQL validation database connection. " + e.getMessage(), e);
			}
		}
	}

}
