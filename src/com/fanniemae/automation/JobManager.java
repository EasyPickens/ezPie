package com.fanniemae.automation;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.automation.actions.Action;
import com.fanniemae.automation.actions.Directory;
import com.fanniemae.automation.actions.ExportDelimited;
import com.fanniemae.automation.actions.LogComment;
import com.fanniemae.automation.actions.DataSet;
import com.fanniemae.automation.actions.LocalTokens;
import com.fanniemae.automation.actions.RunCommand;
import com.fanniemae.automation.actions.SvnCheckout;
import com.fanniemae.automation.common.XmlUtilities;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-16
 * 
 */
public class JobManager {

	protected SessionManager _Session;

	public JobManager(String settingsFilename, String jobFilename) {
		_Session = new SessionManager(settingsFilename, jobFilename);
	}
	
	public String getLogFilename() {
		return _Session.getLogFilename();
	}

	public String runJob() {
		Action act;
		try {
			NodeList nlActions = XmlUtilities.selectNodes(_Session.getJobDefinition(), "*");
			int iLen = nlActions.getLength();
			for (int i = 0; i < iLen; i++) {
				Element eleOperation = (Element) nlActions.item(i);
				switch (eleOperation.getNodeName()) {
				case "RunCommand":
					// Run external command or batch file
					act = new RunCommand(_Session, eleOperation);
					act.execute();
					break;
				case "LocalTokens":
					act = new LocalTokens(_Session, eleOperation);
					act.execute();
					break;
				case "DataSet":
					// Pull data and process
					act = new DataSet(_Session, eleOperation);
					act.execute();
					break;
				case "LogComment":
					act = new LogComment(_Session, eleOperation);
					act.execute();
					break;
				case "Export":
					act = new ExportDelimited(_Session, eleOperation);
					act.execute();
					break;
				case "SvnCheckout":
					act = new SvnCheckout(_Session, eleOperation);
					act.execute();
					break;
				case "Directory" :
					act = new Directory(_Session, eleOperation);
					act.execute();
					break;
				default:
					_Session.addLogMessage("** Warning **", nlActions.item(i).getNodeName(), "Operation not currently supported.");
				}
			}
			_Session.addLogMessage("Completed", "", "Processing completed successfully.");
			return "";
		} catch (Exception ex) {
			_Session.addErrorMessage(ex);
			throw ex;
		}
	}
}
