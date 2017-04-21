/**
 *  
 * Copyright (c) 2015 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPIE/wiki/Fannie-Mae-Open-Source-Licensing-Project
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie;

import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.actions.Action;
import com.fanniemae.ezpie.actions.CastCreateSchemas;
import com.fanniemae.ezpie.actions.CastScan;
import com.fanniemae.ezpie.actions.ComponentScan;
import com.fanniemae.ezpie.actions.Compression;
import com.fanniemae.ezpie.actions.Copy;
import com.fanniemae.ezpie.actions.CreateJavaProjectFiles;
import com.fanniemae.ezpie.actions.DataSet;
import com.fanniemae.ezpie.actions.Delete;
import com.fanniemae.ezpie.actions.DeleteEmpty;
import com.fanniemae.ezpie.actions.Directory;
import com.fanniemae.ezpie.actions.ExecuteSql;
import com.fanniemae.ezpie.actions.ExportDelimited;
import com.fanniemae.ezpie.actions.GetSourceCode;
import com.fanniemae.ezpie.actions.Git;
import com.fanniemae.ezpie.actions.HighlightScan;
import com.fanniemae.ezpie.actions.IfElement;
import com.fanniemae.ezpie.actions.LocalTokens;
import com.fanniemae.ezpie.actions.LogComment;
import com.fanniemae.ezpie.actions.MakeDirectory;
import com.fanniemae.ezpie.actions.Maven;
import com.fanniemae.ezpie.actions.Move;
import com.fanniemae.ezpie.actions.Rename;
import com.fanniemae.ezpie.actions.RunCommand;
import com.fanniemae.ezpie.actions.SendEmail;
import com.fanniemae.ezpie.actions.Sleep;
import com.fanniemae.ezpie.actions.Svn;
import com.fanniemae.ezpie.actions.Tokens;
import com.fanniemae.ezpie.actions.UpdateStatus;
import com.fanniemae.ezpie.actions.VerifyJavaFiles;
import com.fanniemae.ezpie.actions.VersionFile;
import com.fanniemae.ezpie.actions.WebClient;
import com.fanniemae.ezpie.actions.XmlEdit;
import com.fanniemae.ezpie.common.DateUtilities;
import com.fanniemae.ezpie.common.XmlUtilities;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @author Tara Tritt
 * @since 2015-12-16
 * 
 */
public class JobManager {

	protected SessionManager _session;
	protected boolean _stopProcessing = false;
	
	public JobManager(String settingsFilename, String jobFilename, List<String> args) { 
		_session = new SessionManager(settingsFilename, jobFilename, args);
	}

	public String getLogFilename() {
		return _session.getLogFilename();
	}

	public SessionManager getSession() {
		return _session;
	}

	public String runJob() {
		NodeList nlActions = XmlUtilities.selectNodes(_session.getJobDefinition(), "*");
		String result = processActions(nlActions);
		_session.addLogMessage("Completed", "", String.format("Processing completed successfully on %s.", DateUtilities.getCurrentDateTimePretty()));
		return result;
	}

	public String processActions(NodeList nlActions) {
		int iLen = nlActions.getLength();
		if (iLen == 0)
			return "";

		Action act = null;
		try {
			for (int i = 0; i < iLen; i++) {
				Element eleOperation = (Element) nlActions.item(i);
				switch (eleOperation.getNodeName()) {
				case "IncludeSharedElement":
					throw new RuntimeException("Please rename IncludeSharedElement to ImportSharedElement");
				case "SharedElement":
				case "Note":
					continue;
				case "Tokens":
					act = new Tokens(_session, eleOperation);
					break;
				case "RunCommand":
					// Run an external command or batch file
					act = new RunCommand(_session, eleOperation);
					break;
				case "LocalTokens":
					act = new LocalTokens(_session, eleOperation);
					break;
				case "DataSet":
					// Pull data and process
					act = new DataSet(_session, eleOperation);
					break;
				case "LogComment":
					act = new LogComment(_session, eleOperation);
					break;
				case "Export":
					act = new ExportDelimited(_session, eleOperation);
					break;
				case "SvnCheckout":
				case "Svn":
					act = new Svn(_session, eleOperation);
					break;
				case "Directory":
					act = new Directory(_session, eleOperation);
					break;
				case "WebClient":
					act = new WebClient(_session, eleOperation);
					break;
				case "Zip":
				case "UnZip":
					act = new Compression(_session, eleOperation);
					break;
				case "ComponentScan":
					act = new ComponentScan(_session, eleOperation);
					break;
				case "Git":
					act = new Git(_session, eleOperation);
					break;
				case "Maven":
					act = new Maven(_session, eleOperation);
					break;
				case "Copy":
					act = new Copy(_session, eleOperation);
					break;
				case "Move":
					act = new Move(_session, eleOperation);
					break;
				case "Delete":
					act = new Delete(_session, eleOperation);
					break;
				case "DeleteEmpty":
					act = new DeleteEmpty(_session, eleOperation);
					break;
				case "Rename":
					act = new Rename(_session, eleOperation);
					break;
				case "HighlightScan":
					act = new HighlightScan(_session, eleOperation);
					break;
				case "MakeDirectory":
					act = new MakeDirectory(_session, eleOperation);
					break;
				case "VersionFile":
					act = new VersionFile(_session, eleOperation);
					break;
				case "CastScan":
					act = new CastScan(_session, eleOperation);
					break;
				case "CastCreateSchemas":
					act = new CastCreateSchemas(_session, eleOperation);
					break;					
				case "ExecuteSql":
					act = new ExecuteSql(_session, eleOperation);
					break;
				case "Sleep":
					act = new Sleep(_session, eleOperation);
					break;
				case "Stop":
					if (!_session.getAttribute(eleOperation, "Silent").equals("True")) 
						_session.addLogMessage("Stop", "Control Action", "Stopping definition processing. No error.");
					act = null;
					_stopProcessing = true;
					break;
				case "UpdateStatus":
					act = new UpdateStatus(_session, eleOperation);
					break;
				case "XmlEdit":
					act = new XmlEdit(_session, eleOperation);
					break;
				case "If":
					act = new IfElement(_session, eleOperation);
					IfElement condition = (IfElement) act;
					if (condition.evalToBoolean()) {
						NodeList nlSubActions = XmlUtilities.selectNodes(eleOperation, "*");
						processActions(nlSubActions);
					}
					break;
				case "VerifyJavaFiles":
					act = new VerifyJavaFiles(_session, eleOperation);
					break;
				case "CreateJavaProjectFiles":
					act = new CreateJavaProjectFiles(_session, eleOperation);
					break;
				case "SendEmail":
					act = new SendEmail(_session, eleOperation);
					break;	
				case "GetSourceCode":
					act = new GetSourceCode(_session, eleOperation);
					break;
				default:
					_session.addLogMessage("** Warning **", nlActions.item(i).getNodeName(), "Operation not currently supported.");
				}
				
				if (act != null) {
					act.execute();
				}
				
				if (_stopProcessing) 
					break;
			}
			return "";
		} catch (Exception ex) {
			_session.addErrorMessage(ex);
			throw ex;
		}
	}

}
