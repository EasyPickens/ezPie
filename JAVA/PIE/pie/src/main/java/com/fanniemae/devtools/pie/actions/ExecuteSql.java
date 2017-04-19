package com.fanniemae.devtools.pie.actions;

import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.StringUtilities;
import com.fanniemae.devtools.pie.data.connectors.DataConnector;
import com.fanniemae.devtools.pie.data.connectors.SqlConnector;

//@formatter:off
/**
*  
* Copyright (c) 2016 Fannie Mae, All rights reserved.
* This program and the accompany materials are made available under
* the terms of the Fannie Mae Open Source Licensing Project available 
* at https://github.com/FannieMaeOpenSource/ezPIE/wiki/Fannie-Mae-Open-Source-Licensing-Project
* 
* ezPIE is a trademark of Fannie Mae
* 
* @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
* @since 2016-08-16
* 
*/
//@formatter:on

public class ExecuteSql extends Action {

	public ExecuteSql(SessionManager session, Element action) {
		super(session, action, false);
		if (StringUtilities.isNullOrEmpty(_id))
			_id = "LocalData";
	}

	@Override
	public String executeAction() {
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
				if (sqlConnection.eof()) {
					_session.addLogMessage("", "End of Set", "Reached the end of the result set.");
				} else {
					_session.addLogMessage("", "End of Read", "Query returned more data, but only loading values from the first row into tokens.  Use DataSet element to work with multiple rows.");
				}
			}
			sqlConnection.close();
		} catch (Exception ex) {
			throw new RuntimeException("Error running ExecuteSql command. " + ex.getMessage(), ex);
		}
		return null;
	}

}
