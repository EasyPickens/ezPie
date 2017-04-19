//@formatter:off
/**
 *  
 * Copyright (c) 2015 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPIE/wiki/Fannie-Mae-Open-Source-Licensing-Project
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
**/
//@formatter:on

package com.fanniemae.devtools.pie;

import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.devtools.pie.actions.Action;
import com.fanniemae.devtools.pie.actions.CastCreateSchemas;
import com.fanniemae.devtools.pie.actions.CastScan;
import com.fanniemae.devtools.pie.actions.ComponentScan;
import com.fanniemae.devtools.pie.actions.Compression;
import com.fanniemae.devtools.pie.actions.Copy;
import com.fanniemae.devtools.pie.actions.CreateJavaProjectFiles;
import com.fanniemae.devtools.pie.actions.Directory;
import com.fanniemae.devtools.pie.actions.ExecuteSql;
import com.fanniemae.devtools.pie.actions.ExportDelimited;
import com.fanniemae.devtools.pie.actions.GetSourceCode;
import com.fanniemae.devtools.pie.actions.Git;
import com.fanniemae.devtools.pie.actions.HighlightScan;
import com.fanniemae.devtools.pie.actions.IfElement;
import com.fanniemae.devtools.pie.actions.LogComment;
import com.fanniemae.devtools.pie.actions.MakeDirectory;
import com.fanniemae.devtools.pie.actions.Maven;
import com.fanniemae.devtools.pie.actions.Move;
import com.fanniemae.devtools.pie.actions.Rename;
import com.fanniemae.devtools.pie.actions.DataSet;
import com.fanniemae.devtools.pie.actions.Delete;
import com.fanniemae.devtools.pie.actions.DeleteEmpty;
import com.fanniemae.devtools.pie.actions.LocalTokens;
import com.fanniemae.devtools.pie.actions.RunCommand;
import com.fanniemae.devtools.pie.actions.SendEmail;
import com.fanniemae.devtools.pie.actions.Sleep;
import com.fanniemae.devtools.pie.actions.Svn;
import com.fanniemae.devtools.pie.actions.Tokens;
import com.fanniemae.devtools.pie.actions.UpdateStatus;
import com.fanniemae.devtools.pie.actions.VerifyJavaFiles;
import com.fanniemae.devtools.pie.actions.VersionFile;
import com.fanniemae.devtools.pie.actions.WebClient;
import com.fanniemae.devtools.pie.actions.XmlEdit;
import com.fanniemae.devtools.pie.common.DateUtilities;
import com.fanniemae.devtools.pie.common.XmlUtilities;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2015-12-16
 * 
**/
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
