package com.fanniemae.devtools.pie.actions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.ArrayUtilities;
import com.fanniemae.devtools.pie.common.DateUtilities;
import com.fanniemae.devtools.pie.common.FileUtilities;
import com.fanniemae.devtools.pie.common.StringUtilities;
import com.fanniemae.devtools.pie.common.XmlUtilities;

public class CastScan extends RunCommand {

	protected String _connectionProfile;
	protected String _applicationName;
	protected String _version;
	protected String _castFolder;

	protected DateFormat _dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

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
			// Default to Package, analyze, snapshot -- consolidate added once rest tested
			defaultRescanPattern();
		}
	}

	@Override
	public String execute() {
		NodeList castActions = XmlUtilities.selectNodes(_action, "*");
		int length = castActions.getLength();
		for (int i = 0; i < length; i++) {
			Element castAction = (Element) (castActions.item(i));
			String nodeName = castAction.getNodeName();
			_session.addLogMessage(nodeName, String.format("%s Step", _actionName), String.format("Starting the %s step of %s", nodeName, _actionName));
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
				_session.addLogMessage("** Warning **", castAction.getNodeName(), "CastScan does not currently support this processing step.");
			}
		}
		return "";
	}

	protected void packageCode(Element castAction) {
		String templateName = optionalAttribute(castAction, "TemplateName", "default_template");
		String datePackaged = optionalAttribute(castAction, "DatePackaged", _dateFormat.format(new Date()));
		String logFile = optionalAttribute(castAction, "LogFile", FileUtilities.getRandomFilename(_session.getLogPath(), "txt"));

		//@formatter:off
		_arguments = new String[] { "CAST-MS-CLI.exe", "AutomateDelivery", 
				                    "-connectionProfile", StringUtilities.wrapValue(_connectionProfile), 
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
		makeBatchFile();
		_session.addLogMessage("", "CAST Log File", "View packaging and delivery log", "file://" + logFile);
		long start = System.currentTimeMillis();
		super.execute();
		_session.addLogMessage("", "Completed", String.format("Time to package was %s", DateUtilities.elapsedTime(start)));
		
	}

	protected void analyzeCode(Element castAction) {
		String logFile = optionalAttribute(castAction, "LogFile", FileUtilities.getRandomFilename(_session.getLogPath(), "txt"));

		//@formatter:off
		_arguments = new String[] { "CAST-MS-CLI.exe", "RunAnalysis", 
				                    "-connectionProfile", StringUtilities.wrapValue(_connectionProfile), 
				                    "-appli", StringUtilities.wrapValue(_applicationName), 
				                    "-logFilePath", StringUtilities.wrapValue(logFile) };
		//@formatter:on
		_workDirectory = _castFolder;
		_timeout = 0;
		_waitForExit = true;

		_session.addLogMessage("", "Command Line", ArrayUtilities.toCommandLine(_arguments));
		makeBatchFile();
		_session.addLogMessage("", "CAST Log File", "View code analysis log", "file://" + logFile);
		long start = System.currentTimeMillis();
		super.execute();
		_session.addLogMessage("", "Completed", String.format("Time to analyze code was %s", DateUtilities.elapsedTime(start)));
	}

	protected void generateSnapshot(Element castAction) {
		String defaultDate = _dateFormat.format(new Date());
		String snapshotName = optionalAttribute(castAction, "SnapshotName", defaultDate);
		String captureDate = optionalAttribute(castAction, "SnapshotDate", defaultDate);
		String logFile = optionalAttribute(castAction, "LogFile", FileUtilities.getRandomFilename(_session.getLogPath(), "txt"));

		//@formatter:off
		_arguments = new String[] { "CAST-MS-CLI.exe", "GenerateSnapshot", 
				                    "-connectionProfile", StringUtilities.wrapValue(_connectionProfile), 
				                    "-appli", StringUtilities.wrapValue(_applicationName),
				                    "-skipAnalysisJob", "TRUE",
				                    "-version", StringUtilities.wrapValue(_version), 
				                    "-snapshot", StringUtilities.wrapValue(snapshotName),
				                    "-captureDate", StringUtilities.wrapValue(captureDate),
				                    "-logFilePath", StringUtilities.wrapValue(logFile) };
		//@formatter:on
		_workDirectory = _castFolder;
		_timeout = 0;
		_waitForExit = true;

		_session.addLogMessage("", "Command Line", ArrayUtilities.toCommandLine(_arguments));
		makeBatchFile();
		_session.addLogMessage("", "CAST Log File", "View generate snapshot log", "file://" + logFile);
		long start = System.currentTimeMillis();
		super.execute();
		_session.addLogMessage("", "Completed", String.format("Time to generate snapshot was %s", DateUtilities.elapsedTime(start)));
	}

	protected void publishResults(Element castAction) {
		throw new RuntimeException("CAST PublishResults action not available in this version.");
	}

	protected void defaultRescanPattern() {
		// Default to Package, analyze, snapshot
		Element packageCode = _action.getOwnerDocument().createElement("PackageCode");
		_action.appendChild(packageCode);

		Element analyzeCode = _action.getOwnerDocument().createElement("AnalyzeCode");
		_action.appendChild(analyzeCode);

		Element generateSnapshot = _action.getOwnerDocument().createElement("GenerateSnapshot");
		_action.appendChild(generateSnapshot);
	}
}
