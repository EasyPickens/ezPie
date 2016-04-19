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
					act = new SvnCheckout(_session, eleOperation);
					act.execute();
					break;
				case "Directory" :
					act = new Directory(_session, eleOperation);
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
