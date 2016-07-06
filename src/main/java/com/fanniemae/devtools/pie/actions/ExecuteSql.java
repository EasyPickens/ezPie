package com.fanniemae.devtools.pie.actions;

import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.StringUtilities;
import com.fanniemae.devtools.pie.data.connectors.DataConnector;
import com.fanniemae.devtools.pie.data.connectors.SqlConnector;

public class ExecuteSql extends Action {

	public ExecuteSql(SessionManager session, Element action) {
		super(session, action, false);
		if (StringUtilities.isNullOrEmpty(_id))
			_id = "LocalData";
	}

	@Override
	public String execute() {
		try (DataConnector sqlConnection = new SqlConnector(_session, _action, false)) {
			sqlConnection.open();
			String[][] columnNames = sqlConnection.getDataSourceSchema();
			String[][] kvps = new String[columnNames.length][2];
			if (!sqlConnection.eof()) {
				Object[] dataRow = sqlConnection.getDataRow();
				for (int i = 0; i < dataRow.length; i++) {
					String value = dataRow[i] == null ? "" : dataRow[i].toString();
					kvps[i][0] = columnNames[i][0];
					kvps[i][1] = value;
				}
				_session.addTokens(_id, kvps);
			}
			sqlConnection.close();
		} catch (Exception ex) {
			throw new RuntimeException("Error running ExecuteSQL command. " + ex.getMessage(), ex);
		}
		return null;
	}

}
