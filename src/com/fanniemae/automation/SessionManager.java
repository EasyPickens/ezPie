package com.fanniemae.automation;

import java.text.SimpleDateFormat;
import java.util.Date;

//import javax.xml.parsers.DocumentBuilder;
//import javax.xml.bind.annotation.XmlElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.fanniemae.automation.common.CryptoUtilities;
import com.fanniemae.automation.common.FileUtilities;
import com.fanniemae.automation.common.StringUtilities;
import com.fanniemae.automation.common.XmlUtilities;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-15
 * 
 */
public class SessionManager {
	protected String _LogFilename;
	protected String _JobFilename;
	
	protected String _AppPath;
	protected String _DefinitionPath;
	protected String _LogPath;
	protected String _StagingPath;
	protected String _TemplatePath;
	protected String _PathSeparator = System.getProperty("file.separator");
	
	protected int _MemoryLimit = 20;

	protected Document _SettingsDoc;
	protected Element _Settings;
	protected Element _Job;

	protected LogManager _Log;
	protected TokenManager _Tokenizer;

	public SessionManager(String settingsFilename, String jobFilename) {
		Document xSettings = XmlUtilities.loadXmlDefinition(settingsFilename);
		if (xSettings == null)
			throw new RuntimeException("No settings information found.");
		
		_SettingsDoc = xSettings;
		_Settings = xSettings.getDocumentElement();
		Node nodeConfig = XmlUtilities.selectSingleNode(_Settings, "Configuration");
		if (nodeConfig == null)
			throw new RuntimeException("Settings file is missing the Configuration element.  Please update the settings file.");

		Element eleConfig = (Element)nodeConfig; 
		_AppPath = FileUtilities.formatPath(eleConfig.getAttribute("ApplicationPath"), System.getProperty("user.dir"), "ApplicationPath");
		_StagingPath = FileUtilities.formatPath(eleConfig.getAttribute("StagingPath"), String.format("%1$s_Staging", _AppPath), "StagingPath");
		_LogPath = FileUtilities.formatPath(eleConfig.getAttribute("LogPath"), String.format("%1$s_Logs", _AppPath), "LogPath");
		_DefinitionPath = FileUtilities.formatPath(eleConfig.getAttribute("DefinitionPath"), String.format("%1$s_Definitions", _AppPath), "DefinitionPath");
		_TemplatePath = FileUtilities.formatPath(eleConfig.getAttribute("TemplatePath"), String.format("%1$s_Templates", _AppPath), "TemplatePath");
		_LogFilename = String.format("%1$s%2$s_%3$s.html", _LogPath,FileUtilities.getFilenameWithoutExtension(jobFilename), new SimpleDateFormat("yyyyMMdd").format(new Date()));
		
		if (FileUtilities.isInvalidFile(jobFilename)) {
			String sAdjustedDefinitionFilename = _DefinitionPath + jobFilename;
			if (FileUtilities.isValidFile(sAdjustedDefinitionFilename))
				jobFilename = sAdjustedDefinitionFilename;
			else
				throw new RuntimeException(String.format("Definition file %s not found.", jobFilename));
		}
		_JobFilename = jobFilename;

		// Create Debug page.
		_Log = new LogManager(_TemplatePath, _LogFilename);
		_Log.addFileDetails(_JobFilename, "Definition Details");
		_Log.addMessage("Setup Token Dictionary", "Load Tokens", "Read value from settings file.");
		_Tokenizer = new TokenManager(_Settings, _Log);

		Document xJob = XmlUtilities.loadXmlDefinition(_JobFilename);
		if (xJob == null)
			throw new RuntimeException("No settings information found.");

		_Job = xJob.getDocumentElement();
		_Log.addMessage("", "Prepare Definition", "Complete");
		_Log.addMessage("", "Adjusted Size", String.format("%,d bytes", XmlUtilities.getOuterXml(_Job).length()));
	}

	public TokenManager getTokenizer() {
		return _Tokenizer;
	}

	public Element getJobDefinition() {
		return _Job;
	}
	
	public String getStagingPath() {
		return _StagingPath;
	}
	
	public int getMemoryLimit() {
		return _MemoryLimit;
	}

	public String getAttribute(Element ele, String name) {
		return _Tokenizer.getAttribute(ele, name);
	}

	public void addLogMessage(String logGroup, String event, String description) {
		addLogMessage(logGroup, event, description, "");
	}

	public void addLogMessage(String logGroup, String event, String description, String cargo) {
		_Log.addMessage(logGroup, event, description, cargo);
	}

	public void addErrorMessage(Exception ex) {
		_Log.addErrorMessage(ex);
	}

	public Element getConnection(String connectionID) {
		if (StringUtilities.isNullOrEmpty(connectionID))
			return null;
		
		Node nodeConnection = XmlUtilities.selectSingleNode(_Settings, String.format("..//Connections/Connection[@ID='%s']", connectionID));
		if (nodeConnection == null)
			return null;
		return (Element) nodeConnection;
	}

	public String getFilenameHash(String value) {
		return String.format("%1$s%2$s%3$s%4$s.dat", _AppPath, "_DataCache", _PathSeparator, CryptoUtilities.hashValue(value));
	}

	public String resolveTokens(String value) {
		return _Tokenizer.resolveTokens(value, null);
	}

	public String resolveTokens(String value, Object[] aDataRow) {
		return _Tokenizer.resolveTokens(value, aDataRow);
	}

	public void addTokens(String tokenType, Node node) {
		_Tokenizer.addTokens(tokenType, node);
	}
}

