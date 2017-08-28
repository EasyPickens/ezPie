/**
 *  
 * Copyright (c) 2015 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.common.DateUtilities;
import com.fanniemae.ezpie.common.JsonUtilities;
import com.fanniemae.ezpie.common.ProcessActions;
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

	public void addTokens(Map<String, String> newTokens) {
		_session.addTokens(newTokens);
	}

	public void addTokens(String tokenType, Map<String, String> newTokens) {
		_session.addTokens(tokenType, newTokens);
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

	public String getDataJson() {
		NodeList nlActions = XmlUtilities.selectNodes(_session.getJobDefinition(), "*");
		processActions(nlActions);

		_session.addLogMessage("Format Data", "Convert", "Converting datasets to JSON." );
		List<String> dataSets = _session.getDataStreamList();
		Collections.sort(dataSets);
		int length = dataSets.size();
		JSONArray jsonDataSets = new JSONArray();
		for (int i = 0; i < length; i++) {
			String name = dataSets.get(i);
			if (_session.getDataStream(name, true).isInternal()) {
				continue;
			}
			_session.addLogMessage("", "DataSet Returned", name );
			// convert each dataset to JSON.
			jsonDataSets.put(JsonUtilities.convert(name, _session.getDataStream(name, true)));
		}
		_session.addLogMessage("Completed", "", String.format("Processing completed successfully on %s.", DateUtilities.getCurrentDateTimePretty()));
		return jsonDataSets.toString();
	}

	public String processActions(NodeList nlActions) {
		return ProcessActions.run(_session, nlActions, null);
	}

}
