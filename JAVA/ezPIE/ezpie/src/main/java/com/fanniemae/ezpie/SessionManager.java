/**
 *  
 * Copyright (c) 2015 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
**/

package com.fanniemae.ezpie;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.fanniemae.ezpie.common.CryptoUtilities;
import com.fanniemae.ezpie.common.DataStream;
import com.fanniemae.ezpie.common.DataTable;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.Miscellaneous;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.common.XmlUtilities;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2015-12-15
 * 
 */
public class SessionManager {
	protected String _logFilename;
	protected String _jobFilename;

	protected String _appPath;
	protected String _definitionPath;
	protected String _logPath;
	protected String _stagingPath;
	protected String _templatePath;
	protected String _pathSeparator = System.getProperty("file.separator");
	protected String _tokenPrefix = "[";
	protected String _tokenSuffix = "]";

	protected String _jobRescanFilename = null;

	protected int _memoryLimit = 20;
	protected int _cacheMinutes = 30;
	protected int _jobKey = -1;

	protected Document _settingsDoc;
	protected Element _settings;
	protected Element _job;
	protected Element _connScanManager;

	protected LogManager _logger;
	protected TokenManager _tokenizer;

	// protected HashMap<String, String> _dataTokens = null;

	protected Boolean _dataCachingEnabled = false;
	protected Boolean _updateScanManager = false;

	protected Map<String, DataStream> _dataSets = new HashMap<String, DataStream>();

	protected DataTable _codeLocations = null;

	public SessionManager(String settingsFilename, String jobFilename, List<String> args) {
		if (!FileUtilities.isValidFile(settingsFilename)) {
			if (FileUtilities.isValidFile(Miscellaneous.getApplicationRoot() + settingsFilename)) {
				settingsFilename = Miscellaneous.getApplicationRoot() + settingsFilename;
			} else {
				throw new RuntimeException(String.format("Settings file not found in %s", settingsFilename));
			}
		}
		Document xSettings = XmlUtilities.loadXmlDefinition(settingsFilename);
		if (xSettings == null)
			throw new RuntimeException("No settings information found.");

		_settingsDoc = xSettings;
		_settings = xSettings.getDocumentElement();
		Node nodeConfig = XmlUtilities.selectSingleNode(_settings, "Configuration");
		if (nodeConfig == null)
			throw new RuntimeException("Settings file is missing the Configuration element.  Please update the settings file.");

		Element eleConfig = (Element) nodeConfig;

		if ((args != null) && (args.size() > 0)) {
			for (int i = 0; i < args.size(); i++) {
				String[] keyValuePair = args.get(i).split("=");
				if ("DefinitionFile".equals(keyValuePair[0])) {
					_jobRescanFilename = keyValuePair[1];
					break;
				}
			}
		}

		Boolean randomLogFilename = StringUtilities.toBoolean(eleConfig.getAttribute("RandomLogFileName"), true);
		_dataCachingEnabled = StringUtilities.toBoolean(eleConfig.getAttribute("DataCacheEnabled"), true);
		_appPath = FileUtilities.formatPath(eleConfig.getAttribute("ApplicationPath"), System.getProperty("user.dir"), "ApplicationPath");
		_stagingPath = FileUtilities.formatPath(eleConfig.getAttribute("StagingPath"), String.format("%1$s_Staging", _appPath), "StagingPath");
		_logPath = FileUtilities.formatPath(eleConfig.getAttribute("LogPath"), String.format("%1$s_Logs", _appPath), "LogPath");
		_definitionPath = FileUtilities.formatPath(eleConfig.getAttribute("DefinitionPath"), String.format("%1$s_Definitions", _appPath), "DefinitionPath");
		_templatePath = FileUtilities.formatPath(eleConfig.getAttribute("TemplatePath"), String.format("%1$s_Templates", _appPath), "TemplatePath");
		if (!randomLogFilename && StringUtilities.isNotNullOrEmpty(_jobRescanFilename)) {
			_logFilename = String.format("%1$s%2$s.html", _logPath, FileUtilities.getFilenameWithoutExtension(_jobRescanFilename));
		} else if (!randomLogFilename) {
			_logFilename = String.format("%1$s%2$s.html", _logPath, FileUtilities.getFilenameWithoutExtension(jobFilename));

		} else {
			_logFilename = FileUtilities.getRandomFilename(_logPath, "html");
		}

		_cacheMinutes = StringUtilities.toInteger(eleConfig.getAttribute("CacheMinutes"), 30);

		// Create Debug page.
		_logger = new LogManager(_templatePath, _logFilename);
		_logger.addMessage("", "Data Caching", _dataCachingEnabled ? "Enabled" : "Disabled");

		if (FileUtilities.isInvalidFile(jobFilename)) {
			String sAdjustedDefinitionFilename = _definitionPath + jobFilename;
			if (FileUtilities.isValidFile(sAdjustedDefinitionFilename))
				jobFilename = sAdjustedDefinitionFilename;
			else {
				RuntimeException exRun = new RuntimeException(String.format("Definition file %s not found.", jobFilename));
				_logger.addErrorMessage(exRun);
				throw exRun;
			}
		}
		_jobFilename = jobFilename;

		try {
			_logger.addFileDetails(_jobFilename, "Definition Details");
			_logger.addMessage("Setup Token Dictionary", "Load Tokens", "Read value from settings file.");
			_tokenizer = new TokenManager(_settings, _logger);
			_tokenPrefix = _tokenizer.getTokenPrefix();
			_tokenSuffix = _tokenizer.getTokenSuffix();

			Document xmlJobDefinition = XmlUtilities.loadXmlDefinition(_jobFilename);
			if (xmlJobDefinition == null)
				throw new RuntimeException("No settings information found.");

			_job = xmlJobDefinition.getDocumentElement();
			String finalJobDefinition = FileUtilities.writeRandomFile(_logPath, ".txt", XmlUtilities.XMLDocumentToString(xmlJobDefinition));
			// _session.addLogMessage("", "Console Output", String.format("View Console Output (%,d lines)", iLines), "file://" + finalJobDefinition);
			_logger.addMessage("", "Prepared Definition", "View Definition", "file://" + finalJobDefinition);
			_logger.addMessage("", "Adjusted Size", String.format("%,d bytes", XmlUtilities.getOuterXml(_job).length()));
			_tokenizer.addToken("Application", "LogFilename", FileUtilities.getFilenameOnly(_logFilename));
		} catch (Exception ex) {
			_logger.addErrorMessage(ex);
			throw ex;
		}

		// TODO: Remove exception block and make connection optional.
		try {
			_updateScanManager = false;
			_connScanManager = getConnection("JavaScanManager");
			_updateScanManager = StringUtilities.toBoolean(getTokenValue("Configuration", "UpdateScanManager"), false);
		} catch (Exception ex) {
		}

		// String jobKey = getTokenValue("Local","JobKey");
		// if ((_connScanManager != null) && StringUtilities.isNotNullOrEmpty(jobKey)) {
		// _updateScanManager = true;
		//// String key = resolveTokens("[Local.JobKey]");
		//// if (StringUtilities.isNullOrEmpty(key))
		//// throw new RuntimeException("Missing job primary key required to update ScanManager status.");
		//// _jobKey = StringUtilities.toInteger(key, -1);
		// }
	}

	public TokenManager getTokenizer() {
		return _tokenizer;
	}

	public Element getJobDefinition() {
		return _job;
	}

	public String getStagingPath() {
		return _stagingPath;
	}

	public String getLogPath() {
		return _logPath;
	}

	public String getLogFilename() {
		return _logFilename;
	}

	public int getMemoryLimit() {
		return _memoryLimit;
	}

	public String getLineSeparator() {
		return System.lineSeparator();
	}

	public Element getConnectionScanManager() {
		return _connScanManager;
	}

	public Boolean updateScanManager() {
		return _updateScanManager;
	}

	public String getAttribute(Node ele, String name) {
		return getAttribute(ele, name, "");
	}

	public String getAttribute(Element ele, String name) {
		return getAttribute(ele, name, "");
	}

	public String getAttribute(Node ele, String name, String defaultValue) {
		return getAttribute((Element) ele, name, defaultValue);
	}

	public String getAttribute(Element ele, String name, String defaultValue) {
		if (ele == null)
			return "";

		String value = ele.getAttribute(name);

		if (StringUtilities.isNullOrEmpty(value))
			return defaultValue;

		if ((value.indexOf(_tokenPrefix) == -1) || (value.indexOf(_tokenSuffix) == -1))
			return value;

		int iTokenSplit = 0;
		int iTokenEnd = 0;
		String[] aTokens = value.split("\\[");

		for (int i = 0; i < aTokens.length; i++) {
			iTokenSplit = aTokens[i].indexOf('.');
			iTokenEnd = aTokens[i].indexOf(']');
			if ((iTokenSplit == -1) || (iTokenEnd == -1))
				continue;
			if (iTokenSplit > iTokenEnd)
				continue;

			String sFullToken = _tokenPrefix + aTokens[i].substring(0, iTokenEnd + 1);
			String sGroup = aTokens[i].substring(0, iTokenSplit);
			String sKey = aTokens[i].substring(iTokenSplit + 1, iTokenEnd);

			// Skip everything but DataSet tokens - others are resolved in
			// Tokenizer.
			if (!sGroup.equals("DataSet")) {
				continue;
			} else if (!_dataSets.containsKey(sKey)) {
				throw new RuntimeException(String.format("Could not find any DataSet object named %s", sKey));
			} else {
				String dataFilename = _dataSets.get(sKey).getFilename();
				if (StringUtilities.isNullOrEmpty(dataFilename)) {
					value = value.replace(sFullToken, String.format("Memory Stream (%,d bytes)", _dataSets.get(sKey).getSize()));
				} else {
					value = value.replace(sFullToken, _dataSets.get(sKey).getFilename());
				}
			}
		}
		return _tokenizer.resolveTokens(value);
	}

	public void addLogMessage(String logGroup, String event, String description) {
		addLogMessage(logGroup, event, description, "");
	}

	public void addLogMessage(String logGroup, String event, String description, String cargo) {
		_logger.addMessage(logGroup, event, description, cargo);
	}

	public void addLogMessageHtml(String logGroup, String event, String description) {
		addLogMessageHtml(logGroup, event, description, "");
	}

	public void addLogMessageHtml(String logGroup, String event, String description, String cargo) {
		_logger.addHtmlMessage(logGroup, event, description, cargo);
	}

	public void addErrorMessage(Exception ex) {
		_logger.addErrorMessage(ex);
	}

	public Element getConnection(String connectionName) {
		if (StringUtilities.isNullOrEmpty(connectionName))
			return null;

		Node nodeConnection = XmlUtilities.selectSingleNode(_settings, String.format("..//Connections/Connection[@Name='%s']", connectionName));
		if (nodeConnection == null) {
			throw new RuntimeException(String.format("Requested connection %s was not found in the settings file.", connectionName));
		}
		return (Element) nodeConnection;
	}

	public String getFilenameHash(String value) {
		return String.format("%1$s%2$s%3$s%4$s.dat", _appPath, "_DataCache", _pathSeparator, CryptoUtilities.hashValue(value));
	}

	public String getRequiredTokenValue(String tokenType, String tokenKey) {
		String value = getTokenValue(tokenType, tokenKey);
		if (StringUtilities.isNullOrEmpty(value)) {
			throw new RuntimeException(String.format("No value is defined for the [%s.%s] token.", tokenType, tokenKey));
		}
		return value;
	}

	public String getTokenValue(String tokenType, String tokenKey) {
		return _tokenizer.getTokenValue(tokenType, tokenKey);
	}

	public String resolveTokens(String value) {
		return _tokenizer.resolveTokens(value);
	}

	public void addTokens(Node node) {
		_tokenizer.addTokens(node);
	}

	public void addTokens(String tokenType, String[][] kvps) {
		_tokenizer.addTokens(tokenType, kvps);
	}

	public void addTokens(String tokenType, Node node) {
		_tokenizer.addTokens(tokenType, node);
	}

	public void addToken(String tokenType, String key, String value) {
		_tokenizer.addToken(tokenType, key, value);
	}

	public void addLogMessagePreserveLayout(String logGroup, String event, String description) {
		_logger.addMessagePreserveLayout(logGroup, event, description);
	}

	public void addDataSet(String name, DataStream ds) {
		_dataSets.put(name, ds);
	}

	public DataStream getDataStream(String name) {
		if (StringUtilities.isNullOrEmpty(name))
			throw new RuntimeException("Missing required DataSetName value.");
		// addLogMessage("", "DataSetName", name);

		if (!_dataSets.containsKey(name))
			throw new RuntimeException(String.format("DataSetName %s was not found in the list of available data sets.", name));

		DataStream dataStream = _dataSets.get(name);
		if (dataStream.IsMemory()) {
			addLogMessage("", "DataStream Details", String.format("MemoryStream of %,d bytes", dataStream.getSize()));
		} else {
			addLogMessage("", "DataStream Details", String.format("FileStream (%s) of %,d bytes", dataStream.getFilename(), dataStream.getSize()));
		}
		return dataStream;
	}

	public void setCodeLocations(DataTable dt) {
		_codeLocations = dt;
	}

	public DataTable getCodeLocations() {
		return _codeLocations;
	}

	public void setDataTokens(HashMap<String, String> dataTokens) {
		_tokenizer.setDataTokens(dataTokens);
	}

	public void clearDataTokens() {
		_tokenizer.clearDataTokens();
	}

	public String optionalAttribute(Node node, String attributeName, String defaultValue) {
		return optionalAttribute((Element) node, attributeName, defaultValue);
	}

	public String optionalAttribute(Element element, String attributeName) {
		return optionalAttribute(element, attributeName, null);
	}

	public String optionalAttribute(Element element, String attributeName, String defaultValue) {
		String value = getAttribute(element, attributeName);
		if (StringUtilities.isNullOrEmpty(value)) {
			value = resolveTokens(defaultValue);
		} else if ("UserID".equals(attributeName) || "Password".equals(attributeName)) {
			addLogMessage("", attributeName, "{value hidden}");
		} else {
			addLogMessage("", attributeName, value);
		}
		return value;
	}

	public String requiredAttribute(Node node, String attributeName) {
		return requiredAttribute((Element) node, attributeName);
	}

	public String requiredAttribute(Element element, String attributeName) {
		String errorMessage = String.format("Missing a value for %s on the %s element.", attributeName, element.getNodeName());
		return requiredAttribute(element, attributeName, errorMessage);
	}

	public String requiredAttribute(Node node, String attributeName, String errorMessage) {
		return requiredAttribute((Element) node, attributeName, errorMessage);
	}

	public String requiredAttribute(Element element, String attributeName, String errorMessage) {
		String value = getAttribute(element, attributeName);
		if (StringUtilities.isNullOrEmpty(value)) {
			throw new RuntimeException(errorMessage);
		}
		addLogMessage("", attributeName, value);
		return value;
	}

	public Boolean cachingEnabled() {
		return _dataCachingEnabled;
	}

	public int getCacheMinutes() {
		return _cacheMinutes;
	}
}
