package com.fanniemae.devtools.pie.actions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.ArrayUtilities;
import com.fanniemae.devtools.pie.common.DateUtilities;
import com.fanniemae.devtools.pie.common.FileUtilities;
import com.fanniemae.devtools.pie.common.SqlUtilities;
import com.fanniemae.devtools.pie.common.StringUtilities;
import com.fanniemae.devtools.pie.common.XmlUtilities;

public class CastScan extends RunCommand {

	protected String _connectionProfile;
	protected String _applicationName;
	protected String _version;
	protected String _castFolder;

	protected Element _connection;

	protected int _jobKey;

	protected DateFormat _dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public CastScan(SessionManager session, Element action) {
		super(session, action, false);

		_connectionProfile = requiredAttribute("ConnectionProfile");
		_applicationName = requiredAttribute("ApplicationName");
		_version = requiredAttribute("ApplicationVersion");
		_castFolder = optionalAttribute("CastFolder",_session.resolveTokens("@CAST.ProgramFolder~"));

		if (FileUtilities.isInvalidDirectory(_castFolder)) {
			throw new RuntimeException(String.format("CastFolder %s does not exist", _castFolder));
		}

		// If empty child element, build default pattern
		NodeList castCommands = XmlUtilities.selectNodes(_action, "*");
		if (castCommands.getLength() == 0) {
			// Default to Package, analyze, snapshot -- consolidate added once rest tested
			defaultRescanPattern();
		}

		if (_session.updateScanManager()) {
			_connection = _session.getConnection("JavaScanManager");
			String key = _session.resolveTokens("@Local.JobKey~");
			if (StringUtilities.isNullOrEmpty(key))
				throw new RuntimeException("Missing job primary key required to update ScanManager status.");
			_jobKey = StringUtilities.toInteger(key, -1);
		}
	}

	@Override
	public String execute() {
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
			_session.addLogMessage(nodeName, String.format("%s Step", _actionName), String.format("Starting the %s step of %s", nodeName, _actionName));
			params[1][1] = String.format("Started: %s", sdf.format(new Date()));
			switch (nodeName) {
			case "BackupDatabase":
				params[0][1] = "Backup Database";
				SqlUtilities.ExecuteScalar(_connection, sqlCommand , params, _session.updateScanManager());
				backupDatabase(castAction);
				break;
			case "PackageCode":
				params[0][1] = "Package Code";
				SqlUtilities.ExecuteScalar(_connection, sqlCommand , params, _session.updateScanManager());
				packageCode(castAction);
				break;
			case "AnalyzeCode":
				params[0][1] = "Analyze Code";
				SqlUtilities.ExecuteScalar(_connection, sqlCommand , params, _session.updateScanManager());
				analyzeCode(castAction);
				break;
			case "GenerateSnapshot":
				params[0][1] = "Generate Snapshot";
				SqlUtilities.ExecuteScalar(_connection, sqlCommand , params, _session.updateScanManager());
				generateSnapshot(castAction);
				break;
			case "PublishResults":
				params[0][1] = "Publish Results";
				SqlUtilities.ExecuteScalar(_connection, sqlCommand , params, _session.updateScanManager());
				publishResults(castAction);
				break;
			default:
				_session.addLogMessage("** Warning **", castAction.getNodeName(), "CastScan does not currently support this processing step.");
			}
		}
		return "";
	}

	protected void backupDatabase(Element castAction) {
		// Update Status to Backup Database
		_session.addLogMessage("", "Update Status", "Changing status of job to start backup of database.");
		Object[][] params = new Object[1][2];
		params[0][0] = "int";
		params[0][1] = _jobKey;
		
		Calendar endTime = Calendar.getInstance();
		endTime.add(Calendar.HOUR_OF_DAY, 2);
		boolean completed = false;
		boolean backupError = false;
		_session.addLogMessage("", "Waiting", "Waiting up to 2 hours for database backup to complete.");
		long start = System.currentTimeMillis();
		// Wait for status to change to Backup Complete or Error
		try {
			while (Calendar.getInstance().compareTo(endTime) < 0) {
				Object value = SqlUtilities.ExecuteScalar(_connection, _session.resolveTokens("@ScanManager.CheckStatus~"), params, _session.updateScanManager());
				if (value != null) {
					String status = value.toString();
					if (status.toLowerCase().startsWith("error")) {
						backupError = true;
						break;
					} else if (!status.startsWith("Backup Database")) {
						completed = true;
						break;
					}
				} else {
					break;
				}
				Thread.sleep(15000); // sleep for 15 seconds.
			}
		} catch (InterruptedException e) {
			throw new RuntimeException("Database polling thread interrupted.", e);
		}
		if (backupError) {
			throw new RuntimeException("Database backup failed. Check the error log on the CAST database server for details.");
		} else if (!completed) {
			throw new RuntimeException("Database backup did not complete within 2 hours. Check the backup log on the CAST database server for details.");
		}
		_session.addLogMessage("", "Completed", String.format("Time to backup data was %s", DateUtilities.elapsedTime(start)));		
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
		//Miscellaneous.sleep(30);
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
		//Miscellaneous.sleep(30);
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
		//Miscellaneous.sleep(30);
		_session.addLogMessage("", "Completed", String.format("Time to generate snapshot was %s", DateUtilities.elapsedTime(start)));
	}

	protected void publishResults(Element castAction) {
		throw new RuntimeException("CAST PublishResults action not available in this version.");
	}

	protected void defaultRescanPattern() {
		// Default to package, backup database, analyze, snapshot
		Element packageCode = _action.getOwnerDocument().createElement("PackageCode");
		_action.appendChild(packageCode);
		
//		Element backupDatabase = _action.getOwnerDocument().createElement("BackupDatabase");
//		_action.appendChild(backupDatabase);

		Element analyzeCode = _action.getOwnerDocument().createElement("AnalyzeCode");
		_action.appendChild(analyzeCode);

		Element generateSnapshot = _action.getOwnerDocument().createElement("GenerateSnapshot");
		_action.appendChild(generateSnapshot);
	}
}
