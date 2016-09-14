package com.fanniemae.devtools.pie.actions;

import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.SqlUtilities;
import com.fanniemae.devtools.pie.common.StringUtilities;

public class UpdateStatus extends Action {

	protected Element _connection;

	protected String _connectionID;
	protected String _updateCommand;
	protected String _message;

	protected int _jobKey;
	protected Object[][] _parameters;

	public UpdateStatus(SessionManager session, Element action) {
		super(session, action, false);
		_updateCommand = optionalAttribute("Command", "@SelfServiceScan.UpdateStatus~");
		_connectionID = optionalAttribute("ConnectionID", "JavaScanManager");
		_message = requiredAttribute("Message");

		// String key = _session.resolveTokens("@Local.JobKey~");
		// if (StringUtilities.isNullOrEmpty(key))
		// throw new RuntimeException("Missing job primary key required to update ScanManager status.");
		// _jobKey = StringUtilities.toInteger(key, -1);

		if (_session.updateScanManager()) {
			_connection = _session.getConnection(_connectionID);
			String key = _session.resolveTokens("@Local.JobKey~");
			if (StringUtilities.isNullOrEmpty(key))
				throw new RuntimeException("Missing job primary key required to update ScanManager status.");
			_jobKey = StringUtilities.toInteger(key, -1);
		}

		_parameters = new Object[3][2];
		_parameters[0][0] = "string";
		_parameters[0][1] = _message;
		_parameters[1][0] = "string";
		_parameters[1][1] = "";
		_parameters[2][0] = "int";
		_parameters[2][1] = _jobKey;
	}

	@Override
	public String executeAction() {
		SqlUtilities.ExecuteScalar(_connection, _updateCommand, _parameters, true);
		return null;
	}

}
