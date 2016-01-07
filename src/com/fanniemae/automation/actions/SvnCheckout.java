package com.fanniemae.automation.actions;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.automation.SessionManager;
import com.fanniemae.automation.common.FileUtilities;
import com.fanniemae.automation.common.StringUtilities;
import com.fanniemae.automation.common.XmlUtilities;

/**
 * 
 * @author Richard Monson
 * @since 2016-01-04
 * 
 */
public class SvnCheckout extends RunCommand {

	public Map<String, String> _DirectoryURLs = new HashMap<String, String>();
	public String _AppVersion;

	public SvnCheckout(SessionManager session, Element eleAction) {
		super(session, eleAction, false);

		String sAppName = _Session.getAttribute(_Action, "ApplicationName");
		String sAppVersion = _Session.getAttribute(eleAction, "ApplicationVersion");
		String sWorkDirectory = _Session.getAttribute(eleAction, "DestinationPath");
		String sWaitForExit = _Session.getAttribute(eleAction, "WaitForExit");
		String sTimeout = _Session.getAttribute(eleAction, "Timeout");

		if (StringUtilities.isNullOrEmpty(sAppName))
			throw new RuntimeException("No ApplicationName value specified.");
		if (StringUtilities.isNullOrEmpty(sAppVersion))
			throw new RuntimeException("No ApplicationVersion value specified.");
		if (StringUtilities.isNullOrEmpty(sWorkDirectory))
			throw new RuntimeException("No WorkDirectory value specified.");
		
		// Read the branches
		NodeList nlBranches = XmlUtilities.selectNodes(_Action, "SvnDirectory");
		int iLen = nlBranches.getLength();
		if (iLen == 0) {
			throw new RuntimeException("No SvnDirectory URLs specified. Nothing to check out.");
		}
		
		for (int i = 0; i < iLen; i++) {
			String sUrl = _Session.getAttribute(nlBranches.item(i), "URL");
			String sDirName = _Session.getAttribute(nlBranches.item(i), "DirectoryName");
			String sRevision = _Session.getAttribute(nlBranches.item(i), "Revision");
			
			if (StringUtilities.isNullOrEmpty(sUrl)) {
				continue;
			}
			
			if (StringUtilities.isNullOrEmpty(sDirName)) {
				int iPos = sUrl.lastIndexOf('/');
				if ((iPos > -1) && (iPos < sUrl.length() - 1)) {
					sDirName = sUrl.substring(iPos + 1);
				} else {
					throw new RuntimeException(String.format("Could not parse URL (%s) for directory name.", sUrl));
				}
			}
			
			if (StringUtilities.isNotNullOrEmpty(sRevision)) {
				if (!sUrl.endsWith("/")) sUrl += "/";
				sUrl += "@"+sRevision;
			}
			
			_DirectoryURLs.put(sDirName, sUrl);
		}

		// Create application directory structure
		String sCurrentPath = sWorkDirectory;
		try {
			_Session.addLogMessage("", "Work Directory", sCurrentPath);
			new File(sCurrentPath).mkdirs();
			// Create each module directory - delete the old if found.
			Boolean bAddNewLine = false;
			StringBuilder sb = new StringBuilder();
			for (Entry<String, String> kvp : _DirectoryURLs.entrySet()) {
				sCurrentPath = sWorkDirectory + File.separator + kvp.getKey();
				if (FileUtilities.isValidDirectory(sCurrentPath)) {
					FileUtils.deleteDirectory(new File(sCurrentPath));
				}
				if (bAddNewLine)
					sb.append(_Session.getLineSeparator());
				sb.append(sCurrentPath);
				new File(sCurrentPath).mkdirs();
				bAddNewLine = true;
			}
			_Session.addLogMessage("", "Branch Directories", sb.toString());
		} catch (SecurityException ex) {
			_Session.addErrorMessage(ex);
			throw new RuntimeException(String.format("Could not create SVN DestinationPath (%s).", sWorkDirectory));
		} catch (IOException e) {
			_Session.addErrorMessage(e);
			throw new RuntimeException(String.format("Could not delete or create SVN DestinationPath (%s).", sWorkDirectory));
		}

		_WorkDirectory = sWorkDirectory;
		_WaitForExit = StringUtilities.toBoolean(sWaitForExit,true);
		_Timeout = parseTimeout(sTimeout);

		if (StringUtilities.isNotNullOrEmpty(sWaitForExit))
			_Session.addLogMessage("", "Wait For Exit", _WaitForExit.toString());
		if (StringUtilities.isNotNullOrEmpty(sTimeout))
			_Session.addLogMessage("", "Timeout Value", String.format("%,d seconds", _Timeout));
	}

	@Override
	public String execute() {
		String sCurrentPath;
		
		for (Entry<String, String> kvp : _DirectoryURLs.entrySet()) {
			sCurrentPath = _WorkDirectory + File.separator + kvp.getKey();
			_Arguments = new String[] { "svn", "checkout", StringUtilities.wrapValue(kvp.getValue()), StringUtilities.wrapValue(sCurrentPath) };
			_Session.addLogMessage("", "Command Line", createCommandLine(_Arguments));
			super.execute();
		}
		return "";
	}

	protected String createCommandLine(String[] arguments) {
		if ((arguments == null) || (arguments.length == 0))
			return "";

		StringBuilder sb = new StringBuilder();
		int iLen = arguments.length;
		for (int i = 0; i < iLen; i++) {
			if (i > 0)
				sb.append(' ');
			sb.append(arguments[i]);
		}
		return sb.toString();
	}
}
