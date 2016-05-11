package com.fanniemae.devtools.pie.actions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;

import javax.xml.bind.DatatypeConverter;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.FileUtilities;
import com.fanniemae.devtools.pie.common.StringUtilities;
import org.json.JSONArray;

public class ComponentScan extends Action{
	protected Element _conn;
	protected String _connID;
	protected String _url;
	protected String _cliPath;
	protected String _workDirectory;
	protected String _assetID;
	protected String _appName;
	protected String _zipPath;
	protected String _basicUsername;
	protected String _basicPassword;
	protected String _proxyHost;
	protected int _proxyPort;
	protected String _proxyUsername;
	protected String _proxyPassword;
	protected NodeList _columns;

	public ComponentScan(SessionManager session, Element action) {
		super(session, action);
		_connID = _session.getAttribute(action, "ConnectionID");
		_session.addLogMessage("", "ConnectionID", _connID);
		_conn = _session.getConnection(_connID);
		_basicUsername = _session.getAttribute(_conn, "BasicUsername");
		_basicPassword = _session.getAttribute(_conn, "BasicPassword");
		_proxyHost = _session.getAttribute(_conn, "ProxyHost");
		_proxyPort = StringUtilities.toInteger(_session.getAttribute(_conn, "ProxyPort"));
		_proxyUsername = _session.getAttribute(_conn, "ProxyUsername");
		_proxyPassword = _session.getAttribute(_conn, "ProxyPassword");
		_url = _session.getAttribute(_conn, "URL");
		_cliPath = _session.getAttribute(_conn, "CLIPath");
		try {
			File cli = new File(_cliPath);
			_workDirectory = cli.getParent();
		} catch (Exception ex) {
			throw new RuntimeException(String.format("CLI Path Error %s", ex.getMessage()), ex);
		}
		_assetID = _session.getAttribute(action, "AssetID");
		_appName = _session.getAttribute(action, "AppName");
		_zipPath = _session.getAttribute(action, "ZipPath");
	}

	@Override
	public String execute() {
		//zip files beforehand
		//GET request for organization id
		String organizationsStr = sendRESTRequest(false, _url+"/api/v2/organizations", null);	
		JSONObject topJSON = new JSONObject(organizationsStr);
		JSONArray organizations = topJSON.getJSONArray("organizations");
		JSONObject object = (JSONObject) organizations.get(0);
		String organizationID = (String) object.get("id");
		//GET request for applications
		String applicationStr = sendRESTRequest(false, _url+"/api/v2/applications", null);	
		topJSON = new JSONObject(applicationStr);
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
			
			String responseStr = sendRESTRequest(true, _url+"/api/v2/applications", app.toString());
			try{
				topJSON = new JSONObject(responseStr);
				topJSON.get("id");
			} catch (Exception ex) {
				throw new RuntimeException(String.format("Error while sending POST request to create application: %s", ex.getMessage()), ex);
			}
		}
		//Run CLM jar
		RunCommand command = new RunCommand(_session, _action);
		String commandStr = "java -jar "+ _cliPath +" -i " + applicationID + " -s " + _url + " " + _zipPath;
		command._arguments = command.parseCommandLine(commandStr);
		command._workDirectory = _workDirectory;
		_session.addLogMessage("", "Work Directory", _workDirectory);
		_session.addLogMessage("", "Command Line", commandStr);
		command.execute();
		
		return null;
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
	
	protected String sendRESTRequest(boolean post, String urlStr, String body){
		try{
			URL url = new URL(urlStr);
			_session.addLogMessage("", "RestConnector", "REST URL : " + url.toString());
			HttpURLConnection connection;
			
			if(_proxyHost.trim().isEmpty()){
				connection = (HttpURLConnection) url.openConnection();
			} else {
				Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(_proxyHost, _proxyPort)); 
				connection = (HttpURLConnection) url.openConnection(proxy);
				setProxyAuthentication();
			}
			
			if(!_basicUsername.trim().isEmpty()){
				String userpass =_basicUsername + ":" + _basicPassword;
				String basicAuth = "Basic " + DatatypeConverter.printBase64Binary(userpass.getBytes());
				connection.setRequestProperty ("Authorization", basicAuth);
			}
			if(post){
				connection.setRequestProperty("Content-Type", "application/json");
				connection.setRequestMethod("POST");
				connection.setDoOutput(true);
				DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
				wr.writeBytes(body);
				wr.flush();
				wr.close();
				_session.addLogMessage("", "RestConnector", "Post parameters : " + body);
			} else { 
				connection.setRequestMethod("GET");
			}

			int responseCode = connection.getResponseCode();
	
			_session.addLogMessage("", "RestConnector", String.format("Response Code: %,d", responseCode));
			
			String responseStr;
			try(BufferedReader in = new BufferedReader(
			        new InputStreamReader(connection.getInputStream()))){
				String inputLine;
				StringBuffer responseBuffer = new StringBuffer();
				while ((inputLine = in.readLine()) != null) {
					responseBuffer.append(inputLine);
				}
				responseStr = responseBuffer.toString();
			}
			
			Object responseJSON = new JSONTokener(responseStr).nextValue();
			String jsonString = "";
			if (responseJSON instanceof JSONObject){
				jsonString = ((JSONObject) responseJSON).toString(2);
			} else if (responseJSON instanceof JSONArray){
				jsonString = ((JSONArray) responseJSON).toString(2);
			}
			
			//write returned JSON to file in logs folder
			String jsonFilename = FileUtilities.getRandomFilename(_session.getLogPath(), "txt");
			try (Writer writer = new BufferedWriter(new OutputStreamWriter(
			              new FileOutputStream(jsonFilename), "utf-8"))) {
				writer.write(jsonString); 
			}
			
			_session.addLogMessage("", "RestConnector", String.format("View Response"), "file://" + jsonFilename);
			
			return responseStr;
		} catch (JSONException | IOException ex){
			throw new RuntimeException("Error while trying to make REST request: " + ex.getMessage(), ex);
		}
	}
	
	private void setProxyAuthentication(){
		Authenticator authenticator = new Authenticator() {
			public PasswordAuthentication getPasswordAuthentication() {
				return (new PasswordAuthentication(_proxyUsername, _proxyPassword.toCharArray()));
			}
		};
		Authenticator.setDefault(authenticator);
	}
}
