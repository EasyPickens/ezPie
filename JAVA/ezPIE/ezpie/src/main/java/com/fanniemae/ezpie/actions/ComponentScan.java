/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 *
 */

package com.fanniemae.ezpie.actions;

import org.json.JSONObject;
import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.ArrayUtilities;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.common.RestUtilities;
import com.fanniemae.ezpie.common.ZipUtilities;

import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.json.JSONArray;

/**
 *
 * @author Tara Tritt
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2016-05-11
 * 
 */

public class ComponentScan extends RunCommand{
	protected Element _conn;
	protected String _connID;
	protected String _url;
	protected String _cliPath;
	protected String _username;
	protected String _password;
	protected String _source;
	protected String _assetID;
	protected String _appName;
	protected String _orgName;
	protected String _zipPath;
	protected String _batchFilename;
	protected static final String UNKNOWNORGID = "57eaa8da48c84eac94ed01c0884fa157";
	protected static final String POLICYFAILUREOUTPUT = "[ERROR] The IQ Server reports policy failing due to";

	public ComponentScan(SessionManager session, Element action) {
		super(session, action, true);
		_connID = requiredAttribute("ConnectionName");
		_session.addLogMessage("", "ConnectionName", _connID);
		
		_conn = _session.getConnection(_connID);
		_username = requiredAttribute(_conn, "Username");
		_password = requiredAttribute(_conn, "Password");
		_url = requiredAttribute(_conn, "URL");
		_cliPath = requiredAttribute(_conn, "CliPath");

		_assetID = requiredAttribute("AssetID");
		_appName = requiredAttribute("AppName");
		_source = requiredAttribute("Source");	
		_orgName = optionalAttribute("Portfolio", "unknown");
		
		_workDirectory = requiredAttribute("Source").trim();
		if (FileUtilities.isInvalidDirectory(_workDirectory)) {
			throw new PieException(String.format("Source %s is not a valid directory.", _workDirectory));
		}
		
		_zipPath = FileUtilities.getRandomFilename(_session.getStagingPath(), "zip");

		StringBuilder sb = new StringBuilder();
		sb.append(String.format("java -jar %s -i %s -s %s -t release %s", _cliPath, _assetID, _url, _zipPath));

		_session.addLogMessage("", "Command", sb.toString());
		_batchFilename = FileUtilities.writeRandomFile(_session.getStagingPath(), "bat", sb.toString());
		_session.addLogMessage("", "Batch File", _batchFilename);
		_arguments = new String[] { _batchFilename };

	}

	@Override
	public String executeAction(HashMap<String, String> dataTokens) {
		_session.setDataTokens(dataTokens);
		try {
			//zip files and only extract out *.jar and *.dll files
			Pattern[] includePattern = new Pattern[2];
			includePattern[0] = Pattern.compile("(?).jar$");
			includePattern[1] = Pattern.compile("(?).dll$");
			String filelist = ArrayUtilities.toString(ZipUtilities.zip(_source, _zipPath, includePattern, null));
			_session.addLogMessageHtml("", "Files Compressed", filelist);
			_session.addLogMessage("", "Created Zip file with only jar and dll files", _zipPath);
		} catch (IOException ex) {
			_session.addErrorMessage(ex);
		}
		//GET request for organization id
		String organizationsStr = RestUtilities.sendGetRequest(_url+"/api/v2/organizations", _username, _password);	
		_session.addLogMessage("", "Rest Request", String.format("View Response"), "file://" + RestUtilities.writeResponseToFile(organizationsStr, FileUtilities.getRandomFilename(_session.getLogPath(), "txt")));
		JSONObject topJSON = new JSONObject(organizationsStr);
		JSONArray organizations = topJSON.getJSONArray("organizations");
		String organizationID = searchOrganizations(organizations);
		//GET request for applications
		String applicationsStr = RestUtilities.sendGetRequest(_url+"/api/v2/applications", _username, _password);
		_session.addLogMessage("", "Rest Request", String.format("View Response"), "file://" + RestUtilities.writeResponseToFile(applicationsStr, FileUtilities.getRandomFilename(_session.getLogPath(), "txt")));
		topJSON = new JSONObject(applicationsStr);
		JSONArray applications = topJSON.getJSONArray("applications");
		String applicationID = searchApplications(applications);
		
		if(applicationID == null){
			//application doesn't exist 
			//POST request to create application
			applicationID = _assetID;
			JSONObject app = new JSONObject();
			app.put("publicId", _assetID);
			app.put("name", _appName);
			app.put("organizationId", organizationID);
			String responseStr = RestUtilities.sendPostRequest(_url+"/api/v2/applications", app.toString(), _username, _password);
			_session.addLogMessage("", "Rest Request", String.format("View Response"), "file://" + RestUtilities.writeResponseToFile(responseStr, FileUtilities.getRandomFilename(_session.getLogPath(), "txt")));
			try{
				topJSON = new JSONObject(responseStr);
				topJSON.get("id");
			} catch (Exception ex) {
				throw new PieException(String.format("Error while sending POST request to create application: %s", ex.getMessage()), ex);
			}
		}
		
		//running IQ Server Jar to do component scan
		_acceptableErrorOutput = POLICYFAILUREOUTPUT;
		super.executeAction(dataTokens);
		_session.setDataTokens(dataTokens);
		return null;
	}
	
	protected String searchOrganizations(JSONArray orgs){
		for(int i = 0; i < orgs.length(); i++){
			JSONObject org = (JSONObject) orgs.get(i);
			if(((String) org.get("name")).equalsIgnoreCase(_orgName.toLowerCase())){
				return (String) org.get("id");
			}
		}
		//return ID for Unknown Portfolio
		return UNKNOWNORGID;
	}
	
	protected String searchApplications(JSONArray apps){
		for(int i = 0; i < apps.length(); i++){
			JSONObject app = (JSONObject) apps.get(i);
			String publicID = (String) app.get("publicId");
			if(publicID.equals(_assetID)){
				return publicID;
			} else if(((String) app.get("name")).equals(_appName)){
				return publicID;
			}
		}
		return null;
	}
	
}
