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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.ArrayUtilities;
import com.fanniemae.ezpie.common.Constants;
import com.fanniemae.ezpie.common.DateUtilities;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.Miscellaneous;
import com.fanniemae.ezpie.common.SqlUtilities;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.common.XmlUtilities;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-06-15
 * 
 */

public class CastScan extends CastAction {

	protected String _connectionProfile;
	protected String _applicationName;
	protected String _version;

	public CastScan(SessionManager session, Element action) {
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
			throw new RuntimeException(String.format("No value for %sSelfServiceScan.UpdateStatus%s token.", _session.getTokenPrefix(), _session.getTokenSuffix()));
		}

		for (int i = 0; i < length; i++) {
			Element castAction = (Element) (castActions.item(i));
			String nodeName = castAction.getNodeName();
			_session.addLogMessage(nodeName, String.format("%s Step", _actionName), String.format("Starting the %s step of %s (started: %s)", nodeName, _actionName, _sdf.format(new Date())));
			params[1][1] = String.format("Started: %s", sdf.format(new Date()));
			switch (nodeName) {
			case "PackageCode":
				params[0][1] = "Package Code";
				SqlUtilities.ExecuteScalar(_connection, sqlCommand, params, _session.updateScanManager());
				packageCode(castAction);
				break;
			case "BackupDatabase":
				params[0][1] = "Backup Database";
				SqlUtilities.ExecuteScalar(_connection, String.format("UPDATE fnma_measure8.scan_manager SET dblog_name = null WHERE pkey = %d", _jobKey), null, _session.updateScanManager());
				SqlUtilities.ExecuteScalar(_connection, sqlCommand, params, _session.updateScanManager());
				backupDatabase(castAction);
				break;
			case "AnalyzeCode":
				params[0][1] = "Analyze Code";
				SqlUtilities.ExecuteScalar(_connection, sqlCommand, params, _session.updateScanManager());
				analyzeCode(castAction);
				break;
			case "GenerateSnapshot":
				params[0][1] = "Generate Snapshot";
				SqlUtilities.ExecuteScalar(_connection, sqlCommand, params, _session.updateScanManager());
				generateSnapshot(castAction);
				break;
			case "PublishResults":
				params[0][1] = "Publish Results";
				SqlUtilities.ExecuteScalar(_connection, sqlCommand, params, _session.updateScanManager());
				publishResults(castAction);
				break;
			case "LinkApplicationSite":
				params[0][1] = "Link CED Site";
				SqlUtilities.ExecuteScalar(_connection, sqlCommand, params, _session.updateScanManager());
				linkSiteToAAD(castAction);
				break;
			case "ConfigureTransactions":
				params[0][1] = "Configure TCC";
				SqlUtilities.ExecuteScalar(_connection, sqlCommand, params, _session.updateScanManager());
				configureTransactions(castAction);
				break;
			case "ConfigurePreferences":
				params[0][1] = "Add License";
				SqlUtilities.ExecuteScalar(_connection, sqlCommand, params, _session.updateScanManager());
				configurePreferences(castAction);
				break;
			default:
				_session.addLogMessage(Constants.LOG_WARNING_MESSAGE, castAction.getNodeName(), "CastScan does not currently support this processing step.");
			}
		}
		_session.clearDataTokens();
		return "";
	}

	@Override
	protected void initialize() {
		_connectionProfile = requiredAttribute("ConnectionProfile");
		_applicationName = requiredAttribute("ApplicationName");
		_version = requiredAttribute("ApplicationVersion");

		// If empty child element, build default pattern
		NodeList castCommands = XmlUtilities.selectNodes(_action, "*");
		if (castCommands.getLength() == 0) {
			// Default to Package, analyze, snapshot -- consolidate added once rest tested
			defaultRescanPattern();
		}
	}

	protected void backupDatabase(Element castAction) {
		if (_session.updateScanManager()) {

		}
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
		boolean haveDbFilename = false;
		while (Calendar.getInstance().compareTo(endTime) < 0) {
			if (!haveDbFilename) {
				Object logname = SqlUtilities.ExecuteScalar(_connection, _session.getTokenValue("SelfServiceScan", "GetLogFilename"), params, _session.updateScanManager());
				if (logname != null) {
					_session.addLogMessage("", "External Activity Log", "View Database Backup Log", "file://" + (String) logname);
					haveDbFilename = true;
				}
			}
			Object value = SqlUtilities.ExecuteScalar(_connection, _session.getTokenValue("SelfServiceScan", "CheckStatus"), params, _session.updateScanManager());
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
			Miscellaneous.sleep(15); // sleep for 15 seconds.
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

		executeCastAction("View Code Packaging Log", "%s to package source code.", logFile);
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

		executeCastAction("View Code Analysis Log", "%s to analyze code.", logFile);
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
				                    "-ignoreEmptyModule", "TRUE",
				                    "-consolidateMeasures", "FALSE",
				                    "-logFilePath", StringUtilities.wrapValue(logFile) };
		//@formatter:on
		_workDirectory = _castFolder;
		_timeout = 0;
		_waitForExit = true;

		executeCastAction("View Snapshot Log", "%s to generate anaysis snapshot.", logFile);
	}

	protected void publishResults(Element castAction) {
		String logFile = optionalAttribute(castAction, "LogFile", FileUtilities.getRandomFilename(_session.getLogPath(), "txt"));
		String dbDriver = optionalAttribute(castAction, "DbDriver", _session.getRequiredTokenValue("CAST", "DbDriver"));
		String dbUrl = optionalAttribute(castAction, "DbUrl", _session.getRequiredTokenValue("CAST", "DbUrl"));
		String dbSchema = optionalAttribute(castAction, "DbSchema", _session.getRequiredTokenValue("CAST", "DbSchema"));
		String dbAppCentral = optionalAttribute(castAction, "DbCentral", _session.getRequiredTokenValue("LocalData", "dbprefix") + "_central");
		String dbUser = optionalAttribute(castAction, "DbUser", _session.getRequiredTokenValue("CAST", "DbUser"));
		String dbPassword = optionalAttribute(castAction, "DbPassword", _session.getRequiredTokenValue("CAST", "DbPassword"));

		//@formatter:off
		_arguments = new String[] { "AadConsolidation.exe", 
		                            "-driver", StringUtilities.wrapValue(dbDriver), 
		                            "-url", StringUtilities.wrapValue(dbUrl), 
		                            "-schema", StringUtilities.wrapValue(dbSchema), 
		                            "-user", StringUtilities.wrapValue(dbUser), 
		                            "-password", StringUtilities.wrapValue(dbPassword), 
		                            "-remote_url", StringUtilities.wrapValue(dbUrl), 
		                            "-remote_schema", StringUtilities.wrapValue(dbAppCentral),
		                            "-remote_user", StringUtilities.wrapValue(dbUser), 
		                            "-remote_password", StringUtilities.wrapValue(dbPassword),
		                            "-synchronize_indicators", 
		                            "-log_file", StringUtilities.wrapValue(logFile) };		
		
		//@formatter:on
		_workDirectory = FileUtilities.addDirectory(_castFolder, "\\AAD\\CLI\\");
		_timeout = 0;
		_waitForExit = true;

		executeCastAction("View AAD Publish Log", "%s to publish results into AAD.", logFile);
	}

	protected void linkSiteToAAD(Element castAction) {
		String logFile = optionalAttribute(castAction, "LogFile", FileUtilities.getRandomFilename(_session.getLogPath(), "txt"));
		String dbDriver = optionalAttribute(castAction, "DbDriver", _session.getRequiredTokenValue("CAST", "DbDriver"));
		String dbUrl = optionalAttribute(castAction, "DbUrl", _session.getRequiredTokenValue("CAST", "DbUrl"));
		String dbSchema = optionalAttribute(castAction, "DbSchema", _session.getRequiredTokenValue("CAST", "DbSchema"));
		String dbUser = optionalAttribute(castAction, "DbUser", _session.getRequiredTokenValue("CAST", "DbUser"));
		String dbPassword = optionalAttribute(castAction, "DbPassword", _session.getRequiredTokenValue("CAST", "DbPassword"));
		String siteName = optionalAttribute(castAction, "SiteName", _session.getRequiredTokenValue("LocalData", "dbprefix") + "_central.operator");
		String siteUrl = optionalAttribute(castAction, "SiteUrl", _session.getRequiredTokenValue("LocalData", "ced_url"));

		//@formatter:off
		_arguments = new String[] { "AadSite.exe", 
		                            "-driver", StringUtilities.wrapValue(dbDriver), 
		                            "-url", StringUtilities.wrapValue(dbUrl), 
		                            "-schema", StringUtilities.wrapValue(dbSchema), 
		                            "-user", StringUtilities.wrapValue(dbUser), 
		                            "-password", StringUtilities.wrapValue(dbPassword),
		                            "-site_name", StringUtilities.wrapValue(siteName),
		                            "-site_url", StringUtilities.wrapValue(siteUrl),
		                            "-log_file", StringUtilities.wrapValue(logFile) };		
		
		//@formatter:on
		_workDirectory = FileUtilities.addDirectory(_castFolder, "\\AAD\\CLI\\");
		_timeout = 0;
		_waitForExit = true;

		executeCastAction("View Site Link Log", "%s to link the AAD and CED sites.", logFile);
	}

	protected void configureTransactions(Element castAction) {
		String tccTemplate = optionalAttribute(castAction, "TccTemplateFile", _session.getRequiredTokenValue("CAST", "TccTemplateFile"));

		//@formatter:off
		_arguments = new String[] { "CAST-TransactionConfig.exe", 
				                    "-HideGUI",
				                    "-ConnectProfile", StringUtilities.wrapValue(_connectionProfile),
				                    "-ImportTemplatesSetup", StringUtilities.wrapValue(tccTemplate),
				                    "-SaveAfterAutomatedOperations",
				                    "-ExitAfterAutomatedOperations" };		
		//@formatter:on
		_workDirectory = _castFolder;
		_timeout = 0;
		_waitForExit = true;

		executeCastAction("", "%s to configure transactions.", null);
	}

	protected void configurePreferences(Element castAction) {
		String licenseKey = requiredAttribute(castAction, "LicenseKey");
		String deliveryFolder = requiredAttribute(castAction, "DeliveryFolder");
		String deploymentFolder = requiredAttribute(castAction, "DeploymentFolder");

		//@formatter:off
		_arguments = new String[] { "CAST-MS-CLI.exe", 
				                    "ConfigurePlatformPreferences",
				                    "-connectionProfile", StringUtilities.wrapValue(_connectionProfile),
				                    "-licenseKey", StringUtilities.wrapValue(licenseKey),
				                    "-sourceDeliveryFolder", StringUtilities.wrapValue(deliveryFolder),
				                    "-sourceDeploymentFolder", StringUtilities.wrapValue(deploymentFolder) };		
		//@formatter:on
		_workDirectory = _castFolder;
		_timeout = 0;
		_waitForExit = true;

		executeCastAction("", "%s to configure preferences.", null);
	}

	protected void defaultRescanPattern() {
		// Default to package, backup database, analyze, snapshot, publish, linkSite
		Element packageCode = _action.getOwnerDocument().createElement("PackageCode");
		_action.appendChild(packageCode);

		Element backupDatabase = _action.getOwnerDocument().createElement("BackupDatabase");
		_action.appendChild(backupDatabase);

		Element analyzeCode = _action.getOwnerDocument().createElement("AnalyzeCode");
		_action.appendChild(analyzeCode);

		Element generateSnapshot = _action.getOwnerDocument().createElement("GenerateSnapshot");
		_action.appendChild(generateSnapshot);

		// Leaving these steps out of automatic for now. UI provides button to publish information to AAD.
		// Element publishResults = _action.getOwnerDocument().createElement("PublishResults");
		// _action.appendChild(publishResults);
		//
		// Element linkApplicationSite = _action.getOwnerDocument().createElement("LinkApplicationSite");
		// _action.appendChild(linkApplicationSite);
	}

	protected void executeCastAction(String viewLinkLabel, String timeLabel, String logFilename) {
		_session.addLogMessage("", "Command Line", ArrayUtilities.toCommandLine(_arguments));
		makeBatchFile();
		if (StringUtilities.isNotNullOrEmpty(logFilename)) {
			_session.addLogMessage("", "CAST Log File", viewLinkLabel, "file://" + logFilename);
		}
		long start = System.currentTimeMillis();
		super.executeAction(null);
		// Uncomment sleep and comment out executeAction when doing local testing.
		// Miscellaneous.sleep(10);
		_session.addLogMessage("", "Completed", String.format(timeLabel, DateUtilities.elapsedTime(start)));
	}
}
