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

	public Map<String, String> _directoryURLs = new HashMap<String, String>();
	public String _appVersion;

	public SvnCheckout(SessionManager session, Element action) {
		super(session, action, false);

		String sAppName = _session.getAttribute(_action, "ApplicationName");
		String sAppVersion = _session.getAttribute(action, "ApplicationVersion");
		String sWorkDirectory = _session.getAttribute(action, "DestinationPath");
		String sWaitForExit = _session.getAttribute(action, "WaitForExit");
		String sTimeout = _session.getAttribute(action, "Timeout");

		if (StringUtilities.isNullOrEmpty(sAppName))
			throw new RuntimeException("No ApplicationName value specified.");
		if (StringUtilities.isNullOrEmpty(sAppVersion))
			throw new RuntimeException("No ApplicationVersion value specified.");
		if (StringUtilities.isNullOrEmpty(sWorkDirectory))
			throw new RuntimeException("No WorkDirectory value specified.");
		
		// Read the branches
		NodeList nlBranches = XmlUtilities.selectNodes(_action, "SvnDirectory");
		int iLen = nlBranches.getLength();
		if (iLen == 0) {
			throw new RuntimeException("No SvnDirectory URLs specified. Nothing to check out.");
		}
		
		for (int i = 0; i < iLen; i++) {
			String sUrl = _session.getAttribute(nlBranches.item(i), "URL");
			String sDirName = _session.getAttribute(nlBranches.item(i), "DirectoryName");
			String sRevision = _session.getAttribute(nlBranches.item(i), "Revision");
			
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
			
			_directoryURLs.put(sDirName, sUrl);
		}

		// Create application directory structure
		String sCurrentPath = sWorkDirectory;
		try {
			_session.addLogMessage("", "Work Directory", sCurrentPath);
			new File(sCurrentPath).mkdirs();
			// Create each module directory - delete the old if found.
			boolean bAddNewLine = false;
			StringBuilder sb = new StringBuilder();
			for (Entry<String, String> kvp : _directoryURLs.entrySet()) {
				sCurrentPath = sWorkDirectory + File.separator + kvp.getKey();
				if (FileUtilities.isValidDirectory(sCurrentPath)) {
					FileUtils.deleteDirectory(new File(sCurrentPath));
				}
				if (bAddNewLine)
					sb.append(_session.getLineSeparator());
				sb.append(sCurrentPath);
				new File(sCurrentPath).mkdirs();
				bAddNewLine = true;
			}
			_session.addLogMessage("", "Branch Directories", sb.toString());
		} catch (SecurityException ex) {
			_session.addErrorMessage(ex);
			throw new RuntimeException(String.format("Could not create SVN DestinationPath (%s).", sWorkDirectory));
		} catch (IOException e) {
			_session.addErrorMessage(e);
			throw new RuntimeException(String.format("Could not delete or create SVN DestinationPath (%s).", sWorkDirectory));
		}

		_workDirectory = sWorkDirectory;
		_waitForExit = StringUtilities.toBoolean(sWaitForExit,true);
		_timeout = parseTimeout(sTimeout);

		if (StringUtilities.isNotNullOrEmpty(sWaitForExit))
			_session.addLogMessage("", "Wait For Exit", _waitForExit.toString());
		if (StringUtilities.isNotNullOrEmpty(sTimeout))
			_session.addLogMessage("", "Timeout Value", String.format("%,d seconds", _timeout));
	}

	@Override
	public String execute() {
		String sCurrentPath;
		
		for (Entry<String, String> kvp : _directoryURLs.entrySet()) {
			sCurrentPath = _workDirectory + File.separator + kvp.getKey();
			_arguments = new String[] { "svn", "checkout", StringUtilities.wrapValue(kvp.getValue()), StringUtilities.wrapValue(sCurrentPath) };
			_session.addLogMessage("", "Command Line", createCommandLine(_arguments));
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
