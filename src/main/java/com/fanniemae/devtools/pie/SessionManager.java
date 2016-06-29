package com.fanniemae.devtools.pie;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.fanniemae.devtools.pie.common.CryptoUtilities;
import com.fanniemae.devtools.pie.common.DataStream;
import com.fanniemae.devtools.pie.common.FileUtilities;
import com.fanniemae.devtools.pie.common.StringUtilities;
import com.fanniemae.devtools.pie.common.XmlUtilities;

/**
 * 
 * @author Richard Monson
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

	protected int _memoryLimit = 20;

	protected Document _settingsDoc;
	protected Element _settings;
	protected Element _job;

	protected LogManager _logger;
	protected TokenManager _tokenizer;

	protected Map<String, DataStream> _dataSets = new HashMap<String, DataStream>();

	public SessionManager(String settingsFilename, String jobFilename) {
		Document xSettings = XmlUtilities.loadXmlDefinition(settingsFilename);
		if (xSettings == null)
			throw new RuntimeException("No settings information found.");

		_settingsDoc = xSettings;
		_settings = xSettings.getDocumentElement();
		Node nodeConfig = XmlUtilities.selectSingleNode(_settings, "Configuration");
		if (nodeConfig == null)
			throw new RuntimeException("Settings file is missing the Configuration element.  Please update the settings file.");

		Element eleConfig = (Element) nodeConfig;
		_appPath = FileUtilities.formatPath(eleConfig.getAttribute("ApplicationPath"), System.getProperty("user.dir"), "ApplicationPath");
		_stagingPath = FileUtilities.formatPath(eleConfig.getAttribute("StagingPath"), String.format("%1$s_Staging", _appPath), "StagingPath");
		_logPath = FileUtilities.formatPath(eleConfig.getAttribute("LogPath"), String.format("%1$s_Logs", _appPath), "LogPath");
		_definitionPath = FileUtilities.formatPath(eleConfig.getAttribute("DefinitionPath"), String.format("%1$s_Definitions", _appPath), "DefinitionPath");
		_templatePath = FileUtilities.formatPath(eleConfig.getAttribute("TemplatePath"), String.format("%1$s_Templates", _appPath), "TemplatePath");
		// _LogFilename = String.format("%1$s%2$s_%3$s.html", _LogPath,
		// FileUtilities.getFilenameWithoutExtension(jobFilename), new
		// SimpleDateFormat("yyyyMMdd").format(new Date()));
		_logFilename = FileUtilities.getRandomFilename(_logPath, "html");

		if (FileUtilities.isInvalidFile(jobFilename)) {
			String sAdjustedDefinitionFilename = _definitionPath + jobFilename;
			if (FileUtilities.isValidFile(sAdjustedDefinitionFilename))
				jobFilename = sAdjustedDefinitionFilename;
			else
				throw new RuntimeException(String.format("Definition file %s not found.", jobFilename));
		}
		_jobFilename = jobFilename;

		// Create Debug page.
		_logger = new LogManager(_templatePath, _logFilename);
		try {
			_logger.addFileDetails(_jobFilename, "Definition Details");
			_logger.addMessage("Setup Token Dictionary", "Load Tokens", "Read value from settings file.");
			_tokenizer = new TokenManager(_settings, _logger);

			Document xJob = XmlUtilities.loadXmlDefinition(_jobFilename);
			if (xJob == null)
				throw new RuntimeException("No settings information found.");

			_job = xJob.getDocumentElement();
			_logger.addMessage("", "Prepare Definition", "Complete");
			_logger.addMessage("", "Adjusted Size", String.format("%,d bytes", XmlUtilities.getOuterXml(_job).length()));
		} catch (Exception ex) {
			_logger.addErrorMessage(ex);
			throw ex;
		}
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
		String value = ele.getAttribute(name);

		if (StringUtilities.isNullOrEmpty(value))
			return defaultValue;

		if ((value.indexOf("@") == -1) || (value.indexOf("~") == -1))
			return value;

		int iTokenSplit = 0;
		int iTokenEnd = 0;
		String[] aTokens = value.split("@");

		for (int i = 0; i < aTokens.length; i++) {
			iTokenSplit = aTokens[i].indexOf('.');
			iTokenEnd = aTokens[i].indexOf('~');
			if ((iTokenSplit == -1) || (iTokenEnd == -1))
				continue;
			if (iTokenSplit > iTokenEnd) continue;

			String sFullToken = "@" + aTokens[i].substring(0, iTokenEnd + 1);
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

	public Element getConnection(String connectionID) {
		if (StringUtilities.isNullOrEmpty(connectionID))
			return null;

		Node nodeConnection = XmlUtilities.selectSingleNode(_settings, String.format("..//Connections/Connection[@ID='%s']", connectionID));
		if (nodeConnection == null)
			return null;
		return (Element) nodeConnection;
	}

	public String getFilenameHash(String value) {
		return String.format("%1$s%2$s%3$s%4$s.dat", _appPath, "_DataCache", _pathSeparator, CryptoUtilities.hashValue(value));
	}

	public String resolveTokens(String value) {
		return _tokenizer.resolveTokens(value, null);
	}

	public String resolveTokens(String value, Object[] aDataRow) {
		return _tokenizer.resolveTokens(value, aDataRow);
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

	public void addDataSet(String id, DataStream ds) {
		_dataSets.put(id, ds);
	}

	public DataStream getDataStream(String dataSetID) {
		if (StringUtilities.isNullOrEmpty(dataSetID))
			throw new RuntimeException("Missing required DataSetID value.");
		addLogMessage("", "DataSetID", dataSetID);

		if (!_dataSets.containsKey(dataSetID))
			throw new RuntimeException(String.format("DataSetID %s was not found in the list of available data sets.", dataSetID));

		DataStream dataStream = _dataSets.get(dataSetID);
		if (dataStream.IsMemory()) {
			addLogMessage("", "DataStream Details", String.format("MemoryStream of %,d bytes", dataStream.getSize()));
		} else {
			addLogMessage("", "DataStream Details", String.format("FileStream (%s) of %,d bytes", dataStream.getFilename(), dataStream.getSize()));
		}
		return dataStream;
	}
}
