package com.fanniemae.devtools.pie.actions;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.FileUtilities;
import com.fanniemae.devtools.pie.common.SqlUtilities;
import com.fanniemae.devtools.pie.common.StringUtilities;
import com.fanniemae.devtools.pie.common.XmlUtilities;

public class CastOnboard extends CastAction {

	protected Element _castConnection;

	public CastOnboard(SessionManager session, Element action) {
		super(session, action);
	}

	@Override
	public String executeAction() {
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
		String sqlCommand = _session.resolveTokens("@ScanManager.UpdateStatus~");
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
				createSchema(castAction, "mngt", "MngtTemplate");
				break;
			default:
				_session.addLogMessage("** Warning **", castAction.getNodeName(), "CastOnboard does not currently support this processing step.");
			}
		}
		return "";
	}

	@Override
	protected void initialize() {
		String connectionID = optionalAttribute("ConnectionID", null);
		if (StringUtilities.isNullOrEmpty(connectionID)) {
			connectionID = _session.getRequiredTokenValue("CAST", "CastDatabase");
		}
		_castConnection = _session.getConnection(connectionID);
		
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
		String logFile = optionalAttribute(castAction, "LogFile", FileUtilities.getRandomFilename(_session.getLogPath(), "txt"));
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
		_timeout = 0;
		_waitForExit = true;

		executeCastAction("View Create Schema Log", "%s to configure schema.", logFile);
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
