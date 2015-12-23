package com.fanniemae.automation;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.automation.actions.DataSet;
import com.fanniemae.automation.actions.LocalTokens;
import com.fanniemae.automation.actions.RunCommand;

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

	public String runJob() {
		try {
			NodeList nlOperations = _Session.getJobDefinition().getElementsByTagName("*"); // .getChildNodes();
			int iLen = nlOperations.getLength();
			for (int i = 0; i < iLen; i++) {
				Element eleOperation = (Element) nlOperations.item(i);
				switch (eleOperation.getNodeName()) {
				case "RunCommand":
					// Run external command or batch file
					RunCommand rc = new RunCommand(_Session, eleOperation);
					rc.execute();
					break;
				case "LocalTokens":
					LocalTokens lc = new LocalTokens(_Session, eleOperation);
					lc.execute();
					break;
				case "DataSet":
					// Pull data and process
					DataSet ds = new DataSet(_Session, eleOperation);
					ds.execute();
					break;
				default:
					_Session.addLogMessage("** Warning **", nlOperations.item(i).getNodeName(), "Operation not currently supported.");
				}
			}
			return "";
		} catch (Exception ex) {
			_Session.addErrorMessage(ex);
			throw ex;
		}
	}
}
