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

package com.fanniemae.ezpie.actions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.Constants;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.SqlUtilities;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.common.XmlUtilities;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-08-30
 *
 */

public class CastCreateSchemas extends CastAction {
	protected Element _castConnection;

	public CastCreateSchemas(SessionManager session, Element action) {
		super(session, action);
	}

	@Override
	public String executeAction(HashMap<String, String> dataTokens) {
		_session.setDataTokens(dataTokens);
		SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy HH:mm:ss");
		Object[][] params = new Object[3][2];
		params[0][0] = "string";
		params[0][1] = "Starting";
		params[1][0] = "string";
		params[1][1] = "";
		params[2][0] = "int";
		params[2][1] = _jobKey;

		NodeList castActions = XmlUtilities.selectNodes(_action, "*");
		int length = castActions.getLength();
		String sqlCommand = _session.getTokenValue("SelfServiceScan", "UpdateStatus");
		if (_session.updateScanManager() && StringUtilities.isNullOrEmpty(sqlCommand)) {
			throw new RuntimeException(String.format("No value for %sSelfServiceScan.UpdateStatus%s token.",_session.getTokenPrefix(), _session.getTokenSuffix()));
		}
		for (int i = 0; i < length; i++) {
			Element castAction = (Element) (castActions.item(i));
			String nodeName = castAction.getNodeName();
			_session.addLogMessage(nodeName, String.format("%s Step", _actionName), String.format("Starting the %s step of %s (started: %s)", nodeName, _actionName, _sdf.format(new Date())));
			params[1][1] = String.format("Started: %s", sdf.format(new Date()));
			switch (nodeName) {
			case "CreateCentral":
				params[0][1] = "Create Central Schema";
				SqlUtilities.ExecuteScalar(_connection, sqlCommand, params, _session.updateScanManager());
				createSchema(castAction, "central", "CentralTemplate");
				break;
			case "CreateLocal":
				params[0][1] = "Create Local Schema";
				SqlUtilities.ExecuteScalar(_connection, sqlCommand, params, _session.updateScanManager());
				createSchema(castAction, "local", "LocalTemplate");
				break;
			case "CreateManagement":
				params[0][1] = "Create Management Schema";
				SqlUtilities.ExecuteScalar(_connection, sqlCommand, params, _session.updateScanManager());
				createSchema(castAction, "mngt", "ManagementTemplate");
				break;
			default:
				_session.addLogMessage(Constants.LOG_WARNING_MESSAGE, castAction.getNodeName(), "CastOnboard does not currently support this processing step.");
			}
		}
		_session.clearDataTokens();
		return "";
	}

	@Override
	protected void initialize() {
		String connectionID = optionalAttribute("ConnectionName", null);
		if (StringUtilities.isNullOrEmpty(connectionID)) {
			connectionID = _session.getRequiredTokenValue("CAST", "CastDatabase");
		}
		_castConnection = _session.getConnection(connectionID);
		
		String dbprefix = optionalAttribute("DbPrefix", _session.getTokenValue("LocalData", "dbprefix"));
		_session.addToken("LocalData", "dbprefix", dbprefix);
		
		// If empty child element, build default pattern
		NodeList castCommands = XmlUtilities.selectNodes(_action, "*");
		if (castCommands.getLength() == 0) {
			// Default to Package, analyze, snapshot -- consolidate added once rest tested
			defaultCastOnBoardPattern();
		}
	}

	protected void createSchema(Element castAction, String dbsuffix, String tokenKey) {
		String dbprefix = optionalAttribute(castAction, "DbPrefix", _session.getTokenValue("LocalData", "dbprefix"));
		String schemaTemplate = optionalAttribute(castAction, "SchemaTemplate", _session.getTokenValue("CAST", tokenKey));
		String logFile = optionalAttribute(castAction, "LogFile", FileUtilities.getRandomFilename(_session.getTokenValue("Configuration", "ApplicationPath")+"\\_Logs", "txt"));
		String tempXml = FileUtilities.getRandomFilename(_session.getStagingPath(), "xml");

		if (FileUtilities.isInvalidFile(schemaTemplate)) {
			throw new RuntimeException(String.format("Template file %s not found.", schemaTemplate));
		} else if (StringUtilities.isNullOrEmpty(dbprefix)) {
			throw new RuntimeException("No database schema prefix defined.");
		} else if ("".equals(_session.getTokenValue("LocalData", "dbprefix"))) {
			_session.addToken("LocalData", "dbprefix", dbprefix);
		}

		String xmlContents = _session.resolveTokens(FileUtilities.loadFile(schemaTemplate));
		FileUtilities.writeFile(tempXml, xmlContents);

		String sqlCommand = String.format("CREATE SCHEMA %s_%s AUTHORIZATION \"operator\"", _session.getRequiredTokenValue("LocalData", "dbprefix").toLowerCase(), dbsuffix);
		_session.addLogMessage("", "Create Schema", sqlCommand);
		SqlUtilities.ExecuteScalar(_castConnection, sqlCommand, null);

		_session.addLogMessage("", "Add Technologies", "Installing default technologies.");
		//@formatter:off
		_arguments = new String[] { "Servman.exe", 
				                    String.format("-INSTALL_CONFIG_FILE(%s)",tempXml),
				                    String.format("-LOG(%s)", logFile) };
		//@formatter:on
		_workDirectory = _castFolder;
		_session.addLogMessage("", "Work Directory", _workDirectory);
		_timeout = 0;
		_waitForExit = true;

		executeCastAction("View Create Schema Log", "%s to configure schema.", null);
		FileUtilities.deleteFile(tempXml);
	}
	
	protected void defaultCastOnBoardPattern() {
		// Default to package, backup database, analyze, snapshot, publish, linkSite
		Element createCentral = _action.getOwnerDocument().createElement("CreateCentral");
		_action.appendChild(createCentral);

		Element createLocal = _action.getOwnerDocument().createElement("CreateLocal");
		_action.appendChild(createLocal);

		Element createMngt = _action.getOwnerDocument().createElement("CreateManagement");
		_action.appendChild(createMngt);
	}

}
