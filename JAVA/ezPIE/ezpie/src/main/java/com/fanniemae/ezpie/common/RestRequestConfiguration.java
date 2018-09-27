/**
 *  
 * Copyright (c) 2018 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.common;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

/**
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2018-02-25
 * 
 */

public class RestRequestConfiguration {

	private String _url;
	private String _proxyHost;
	private int _proxyPort;
	private String _proxyUsername;
	private String _proxyPassword;
	private String _username;
	private String _password;
	private String _requestMethod = "GET";
	private String _messageBody = "";
	private Map<String, String> _requestProperties = null;
	private boolean _validateCerfificate = true;

	public RestRequestConfiguration() {
		_requestProperties = new HashMap<String, String>();
	}

	public String getUrl() {
		return _url;
	}

	public void setUrl(String url) {
		_url = url;
	}

	public String getProxyHost() {
		return _proxyHost;
	}

	public void setProxyHost(String proxyHost) {
		_proxyHost = proxyHost;
	}

	public int getProxyPort() {
		return _proxyPort;
	}

	public void setProxyPort(String proxyPort) {
		if ((proxyPort == null) || proxyPort.isEmpty()) {
			_proxyPort = 80;
		} else {
			_proxyPort = Integer.parseInt(proxyPort);
		}
	}

	public String getProxyUsername() {
		return _proxyUsername;
	}

	public void setProxyUsername(String proxyUsername) {
		_proxyUsername = proxyUsername;
	}

	public String getProxyPassword() {
		return _proxyPassword;
	}

	public void setProxyPassword(String proxyPassword) {
		_proxyPassword = proxyPassword;
	}

	public String getUsername() {
		return _username;
	}

	public void setUsername(String username) {
		_username = username;
	}

	public String getPassword() {
		return _password;
	}

	public void setPassword(String password) {
		_password = password;
	}

	public String getRequestMethod() {
		return _requestMethod;
	}

	public void setRequestMethod(String requestMethod) {
		_requestMethod = requestMethod;
	}

	public String getMessageBody() {
		return _messageBody;
	}

	public void setMessageBody(String messageBody) {
		_messageBody = messageBody;
	}

	public boolean getValidateCerfificate() {
		return _validateCerfificate;
	}

	public void setValidateCerfificate(boolean validateCerfificate) {
		_validateCerfificate = validateCerfificate;
	}

	public void clearRequestProperties() {
		_requestProperties.clear();
	}

	public Map<String, String> getRequestProperties() {
		if ("POST".equals(_requestMethod) && !_requestProperties.containsKey("Content-Type")) {
			_requestProperties.put("Content-Type", "application/json");
		}

		if ((_username != null) && !_username.isEmpty()) {
			_requestProperties.put("Authorization", "Basic " + DatatypeConverter.printBase64Binary(String.format("%s:%s", _username, _password).getBytes()));
		}
		return _requestProperties;
	}

	public void putRequestProperty(String key, String value) {
		_requestProperties.put(key, value);
	}

	public void removeRequestProperty(String key) {
		if (_requestProperties.containsKey(key)) {
			_requestProperties.remove(key);
		}
	}
}
