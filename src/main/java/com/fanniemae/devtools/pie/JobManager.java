package com.fanniemae.devtools.pie;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.devtools.pie.actions.Action;
import com.fanniemae.devtools.pie.actions.ComponentScan;
import com.fanniemae.devtools.pie.actions.Compression;
import com.fanniemae.devtools.pie.actions.Copy;
import com.fanniemae.devtools.pie.actions.Directory;
import com.fanniemae.devtools.pie.actions.ExportDelimited;
import com.fanniemae.devtools.pie.actions.Git;
import com.fanniemae.devtools.pie.actions.HighlightScan;
import com.fanniemae.devtools.pie.actions.LogComment;
import com.fanniemae.devtools.pie.actions.Maven;
import com.fanniemae.devtools.pie.actions.Move;
import com.fanniemae.devtools.pie.actions.Rename;
import com.fanniemae.devtools.pie.actions.DataSet;
import com.fanniemae.devtools.pie.actions.Delete;
import com.fanniemae.devtools.pie.actions.LocalTokens;
import com.fanniemae.devtools.pie.actions.RunCommand;
import com.fanniemae.devtools.pie.actions.Svn;
import com.fanniemae.devtools.pie.actions.WebClient;
import com.fanniemae.devtools.pie.common.XmlUtilities;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-16
 * 
 */
public class JobManager {

	protected SessionManager _session;

	public JobManager(String settingsFilename, String jobFilename) {
		_session = new SessionManager(settingsFilename, jobFilename);
	}
	
	public String getLogFilename() {
		return _session.getLogFilename();
	}

	public String runJob() {
		Action act;
		try {
			NodeList nlActions = XmlUtilities.selectNodes(_session.getJobDefinition(), "*");
			int iLen = nlActions.getLength();
			for (int i = 0; i < iLen; i++) {
				Element eleOperation = (Element) nlActions.item(i);
				switch (eleOperation.getNodeName()) {
				case "RunCommand":
					// Run external command or batch file
					act = new RunCommand(_session, eleOperation);
					act.execute();
					break;
				case "LocalTokens":
					act = new LocalTokens(_session, eleOperation);
					act.execute();
					break;
				case "DataSet":
					// Pull data and process
					act = new DataSet(_session, eleOperation);
					act.execute();
					break;
				case "LogComment":
					act = new LogComment(_session, eleOperation);
					act.execute();
					break;
				case "Export":
					act = new ExportDelimited(_session, eleOperation);
					act.execute();
					break;
				case "SvnCheckout":
					act = new Svn(_session, eleOperation);
					act.execute();
					break;
				case "Directory" :
					act = new Directory(_session, eleOperation);
					act.execute();
					break;
				case "WebClient":
					act = new WebClient(_session, eleOperation);
					act.execute();
					break;
				case "Zip":
				case "UnZip":
					act = new Compression(_session, eleOperation);
					act.execute();
					break;
				case "ComponentScan":
					act = new ComponentScan(_session, eleOperation);
					act.execute();
					break;					
				case "Git":
					act = new Git(_session, eleOperation);
					act.execute();
					break;
				case "Svn":
					act = new Svn(_session, eleOperation);
					act.execute();
					break;					
				case "Maven":
					act = new Maven(_session, eleOperation);
					act.execute();
					break;
				case "Copy":
					act = new Copy(_session, eleOperation);
					act.execute();
					break;
				case "Move":
					act = new Move(_session, eleOperation);
					act.execute();
					break;
				case "Delete":
					act = new Delete(_session, eleOperation);
					act.execute();
					break;					
				case "Rename":
					act = new Rename(_session, eleOperation);
					act.execute();
					break;					
				default:
					_session.addLogMessage("** Warning **", nlActions.item(i).getNodeName(), "Operation not currently supported.");
				}
			}
			_session.addLogMessage("Completed", "", "Processing completed successfully.");
			return "";
		} catch (Exception ex) {
			_session.addErrorMessage(ex);
			throw ex;
		}
	}
}
