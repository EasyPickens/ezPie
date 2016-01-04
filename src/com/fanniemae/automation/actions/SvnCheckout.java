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

public class SvnCheckout extends RunCommand {

	public Map<String, String> _DirectoryURLs = new HashMap<String, String>();
	public String _AppVersion;

	public SvnCheckout(SessionManager session, Element eleAction) {
		super(session, eleAction);

		String sAppName = _Session.getAttribute(_Action, "ApplicationName");
		String sAppVersion = _Session.getAttribute(eleAction, "ApplicationVersion");
		String sWorkDirectory = _Session.getAttribute(eleAction, "DestinationPath");

		String sCmdLine = "svn";
		String sWaitForExit = _Session.getAttribute(eleAction, "WaitForExit");
		String sTimeout = _Session.getAttribute(eleAction, "Timeout");

		if (StringUtilities.isNullOrEmpty(sAppName))
			throw new RuntimeException("No ApplicationName value specified.");
		if (StringUtilities.isNullOrEmpty(sAppVersion))
			throw new RuntimeException("No ApplicationVersion value specified.");		
		if (StringUtilities.isNullOrEmpty(sWorkDirectory))
			throw new RuntimeException("No WorkDirectory value specified.");
		if (FileUtilities.isInvalidDirectory(sWorkDirectory))
			throw new RuntimeException(String.format("WorkDirectory (%s) does not exist.",sWorkDirectory));
		
		sWorkDirectory = String.format("%1$s%2$s%3$s%2$sv%4$s", sWorkDirectory, File.separator, sAppName, sAppVersion);

		// Read the branches
		NodeList nlBranches = XmlUtilities.selectNodes(_Action, "SvnDirectory");
		int iLen = nlBranches.getLength();
		if (iLen == 0) {
			throw new RuntimeException("Missing SvnDirectory URL to checkout.");
		}
		for (int i = 0; i < iLen; i++) {
			String sUrl = _Session.getAttribute(nlBranches.item(i), "URL");
			String sDirName = _Session.getAttribute(nlBranches.item(i), "DirectoryName");
			if (StringUtilities.isNullOrEmpty(sUrl)) {
				continue;
			}
			if (StringUtilities.isNullOrEmpty(sDirName)) {
				int iPos = sUrl.lastIndexOf('/');
				if ((iPos > -1) && (iPos < sUrl.length() - 1)) {
					sDirName = sUrl.substring(iPos+1);
				} else {
					throw new RuntimeException("Could not parse URL for directory name.");
				}
			}
			_DirectoryURLs.put(sDirName, sUrl);
		}
		
		// Create application directory
		String sCurrentPath = sWorkDirectory;
		try {
			new File(sCurrentPath).mkdirs();
			// Create each module directory
			for(Entry<String,String> kvp : _DirectoryURLs.entrySet()) {
				sCurrentPath = sWorkDirectory+File.separator+kvp.getKey();
				if (FileUtilities.isValidDirectory(sCurrentPath)) {
					FileUtils.deleteDirectory(new File(sCurrentPath));
				}
				new File(sCurrentPath).mkdirs();
			}
		} catch (SecurityException ex) {
			_Session.addErrorMessage(ex);
			throw new RuntimeException(String.format("Could not create SVN DestinationPath (%s).", sWorkDirectory));
		} catch (IOException e) {
			_Session.addErrorMessage(e);
			throw new RuntimeException(String.format("Could not delete or create SVN DestinationPath (%s).", sWorkDirectory));
		}



		if (StringUtilities.isNullOrEmpty(sCmdLine))
			throw new RuntimeException("No CommandLine value specified.");

		_WorkDirectory = sWorkDirectory;
		_CommandLine = sCmdLine;
		_WaitForExit = true;
		_Timeout = parseTimeout(sTimeout);

		_Session.addLogMessage("", "Work Directory", _WorkDirectory);
		_Session.addLogMessage("", "Command Line", _CommandLine);
		if (StringUtilities.isNotNullOrEmpty(sWaitForExit))
			_Session.addLogMessage("", "Wait For Exit", _WaitForExit.toString());
		if (StringUtilities.isNotNullOrEmpty(sTimeout))
			_Session.addLogMessage("", "Timeout Value", String.format("%,d seconds", _Timeout));

		_Arguments = parseCommandLine(_CommandLine);
	}

	// @Override
	// public String execute() {
	//
	// return "";
	// }
}
