package com.fanniemae.devtools.pie.actions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.ArrayUtilities;
import com.fanniemae.devtools.pie.common.FileUtilities;
import com.fanniemae.devtools.pie.common.StringUtilities;
import com.fanniemae.devtools.pie.common.XmlUtilities;

public class CastScan extends RunCommand {

	protected String _connectionProfile;
	protected String _applicationName;
	protected String _version;
	protected String _castFolder;

	public CastScan(SessionManager session, Element action) {
		super(session, action, false);

		_connectionProfile = requiredAttribute("ConnectionProfile");
		_applicationName = requiredAttribute("ApplicationName");
		_version = requiredAttribute("ApplicationVersion");
		_castFolder = requiredAttribute("CastFolder");

		if (FileUtilities.isInvalidDirectory(_castFolder)) {
			throw new RuntimeException(String.format("CastFolder %s does not exist", _castFolder));
		}

		// If empty child element, build default pattern
		NodeList castCommands = XmlUtilities.selectNodes(_action, "*");
		if (castCommands.getLength() == 0) {
			// Default to Package, analyze, snapshot -- need to add consolidate once working
			defaultRescan();
		}
	}

	@Override
	public String execute() {
		NodeList castActions = XmlUtilities.selectNodes(_action, "*");
		int length = castActions.getLength();
		for (int i = 0; i < length; i++) {
			Element castAction = (Element) (castActions.item(i));
			String nodeName = castAction.getNodeName(); 
			_session.addLogMessage(nodeName, "Process", "Starting to process");
			switch (nodeName) {
			case "PackageCode":
				packageCode(castAction);
				break;
			case "AnalyzeCode":
				analyzeCode(castAction);
				break;
			case "GenerateSnapshot":
				generateSnapshot(castAction);
				break;
			case "PublishResults":
				publishResults(castAction);
				break;
			default:
				_session.addLogMessage("** Warning **", castAction.getNodeName(), "CastScan does not currently support this child action.");
			}
		}
		return "";
	}

	protected void packageCode(Element castAction) {
		String templateName = requiredAttribute(castAction, "TemplateName");
		String datePackaged = requiredAttribute(castAction, "DatePackaged");
		String logFile = requiredAttribute(castAction, "LogFile");

		//@formatter:off
		_arguments = new String[] { "CAST-MS-CLI.exe", "AutomateDelivery", 
				                    "-connectionProfile", _connectionProfile, 
				                    "-appli", StringUtilities.wrapValue(_applicationName), 
				                    "-version", StringUtilities.wrapValue(_version), 
				                    "-fromVersion", StringUtilities.wrapValue(templateName), 
				                    "-date", StringUtilities.wrapValue(datePackaged),
				                    "-logFilePath", StringUtilities.wrapValue(logFile) };
		//@formatter:on
		_workDirectory = _castFolder;
		_timeout = 0;
		_waitForExit = true;
		
		_session.addLogMessage("", "Command Line", ArrayUtilities.toCommandLine(_arguments));
		//super.execute();
	}

	protected void analyzeCode(Element castAction) {
		String logFile = requiredAttribute(castAction, "LogFile");

		//@formatter:off
		_arguments = new String[] { "CAST-MS-CLI.exe", "RunAnalysis", 
				                    "-connectionProfile", _connectionProfile, 
				                    "-appli", StringUtilities.wrapValue(_applicationName), 
				                    "-logFilePath", StringUtilities.wrapValue(logFile) };
		//@formatter:on
		_workDirectory = _castFolder;
		_timeout = 0;
		_waitForExit = true;

		_session.addLogMessage("", "Command Line", ArrayUtilities.toCommandLine(_arguments));
		// super.execute();
	}

	protected void generateSnapshot(Element castAction) {
		String snapshotName = requiredAttribute(castAction, "SnapshotName");
		String captureDate = requiredAttribute(castAction, "SnapshotDate");
		String logFile = requiredAttribute(castAction, "LogFile");

		//@formatter:off
		_arguments = new String[] { "CAST-MS-CLI.exe", "GenerateSnapshot", 
				                    "-connectionProfile", _connectionProfile, 
				                    "-appli", StringUtilities.wrapValue(_applicationName),
				                    "-version", StringUtilities.wrapValue(_version), 
				                    "-snapshot", StringUtilities.wrapValue(snapshotName),
				                    "-captureDate", StringUtilities.wrapValue(captureDate),
				                    "-logFilePath", StringUtilities.wrapValue(logFile) };
		//@formatter:on
		_workDirectory = _castFolder;
		_timeout = 0;
		_waitForExit = true;

		_session.addLogMessage("", "Command Line", ArrayUtilities.toCommandLine(_arguments));
		// super.execute();
	}

	protected void publishResults(Element castAction) {
		throw new RuntimeException("CAST PublishResults action not currently supported.");
	}

	protected void defaultRescan() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		// Default to Package, analyze, snapshot
		Element packageCode = _action.getOwnerDocument().createElement("PackageCode");
		packageCode.setAttribute("TemplateName", "default_template");
		packageCode.setAttribute("DatePackaged", dateFormat.format(new Date()));
		packageCode.setAttribute("LogFile", FileUtilities.getRandomFilename(_session.getLogPath(), "txt"));
		_action.appendChild(packageCode);

		Element analyzeCode = _action.getOwnerDocument().createElement("AnalyzeCode");
		analyzeCode.setAttribute("LogFile", FileUtilities.getRandomFilename(_session.getLogPath(), "txt"));
		_action.appendChild(analyzeCode);

		dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		Element generateSnapshot = _action.getOwnerDocument().createElement("GenerateSnapshot");
		generateSnapshot.setAttribute("SnapshotName", String.format("Computed on %s", dateFormat.format(new Date())));
		generateSnapshot.setAttribute("SnapshotDate", dateFormat.format(new Date()));
		generateSnapshot.setAttribute("LogFile", FileUtilities.getRandomFilename(_session.getLogPath(), "txt"));
		_action.appendChild(generateSnapshot);
	}
}
