/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 *
 * @author Tara Tritt
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-05-30
 * 
 */

public final class RestUtilities {

	private RestUtilities() {
	}

	public static String sendGetRequest(String urlStr) {
		return sendGetRequest(urlStr, null, 0, null, null, null, null);
	}

	public static String sendGetRequest(String urlStr, String username, String password) {
		return sendGetRequest(urlStr, null, 0, null, null, username, password);
	}

	public static String sendGetRequest(String urlStr, String proxyHost, int proxyPort, String proxyUsername, String proxyPassword) {
		return sendGetRequest(urlStr, proxyHost, proxyPort, proxyUsername, proxyPassword, null, null);
	}

	public static String sendGetRequest(String urlStr, String proxyHost, int proxyPort, String proxyUsername, String proxyPassword, String username, String password) {
		return sendRequest(false, urlStr, null, proxyHost, proxyPort, proxyUsername, proxyPassword, username, password);
	}

	public static String sendPostRequest(String urlStr, String body) {
		return sendPostRequest(urlStr, body, null, 0, null, null, null, null);
	}

	public static String sendPostRequest(String urlStr, String body, String username, String password) {
		return sendPostRequest(urlStr, body, null, 0, null, null, username, password);
	}

	public static String sendPostRequest(String urlStr, String body, String proxyHost, int proxyPort, String proxyUsername, String proxyPassword) {
		return sendPostRequest(urlStr, body, proxyHost, proxyPort, proxyUsername, proxyPassword, null, null);
	}

	public static String sendPostRequest(String urlStr, String body, String proxyHost, int proxyPort, String proxyUsername, String proxyPassword, String username, String password) {
		return sendRequest(true, urlStr, body, proxyHost, proxyPort, proxyUsername, proxyPassword, username, password);
	}

	public static String sendRequest(boolean post, String urlStr, String body, String proxyHost, int proxyPort, String proxyUsername, String proxyPassword, String username, String password) {
		try {
			URL url = new URL(urlStr);

			HttpURLConnection connection;
			if (StringUtilities.isNotNullOrEmpty(proxyHost)) {
				Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
				connection = (HttpURLConnection) url.openConnection(proxy);
				setProxyAuthentication(proxyUsername, proxyPassword);
			} else {
				connection = (HttpURLConnection) url.openConnection();
			}

			if (StringUtilities.isNotNullOrEmpty(username)) {
				String userpass = username + ":" + password;
				String basicAuth = "Basic " + DatatypeConverter.printBase64Binary(userpass.getBytes());
				connection.setRequestProperty("Authorization", basicAuth);
			}
			if (post) {
				connection.setRequestProperty("Content-Type", "application/json");
				connection.setRequestMethod("POST");
				connection.setDoOutput(true);
				DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
				wr.writeBytes(body);
				wr.flush();
				wr.close();
			} else {
				connection.setRequestMethod("GET");
			}

			String responseStr;
			try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				String inputLine;
				StringBuffer responseBuffer = new StringBuffer();
				while ((inputLine = in.readLine()) != null) {
					responseBuffer.append(inputLine);
				}
				responseStr = responseBuffer.toString();
			}

			return responseStr;
		} catch (JSONException | IOException ex) {
			throw new RuntimeException("Error while trying to make REST request: " + ex.getMessage(), ex);
		}
	}

	public static String writeResponseToFile(String responseStr, String filename) {
		Object responseJSON = new JSONTokener(responseStr).nextValue();
		String jsonString = "";
		if (responseJSON instanceof JSONObject) {
			jsonString = ((JSONObject) responseJSON).toString(2);
		} else if (responseJSON instanceof JSONArray) {
			jsonString = ((JSONArray) responseJSON).toString(2);
		}

		// write returned JSON to file in logs folder
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "utf-8"))) {
			writer.write(jsonString);
		} catch (IOException ex) {
			// TODO Auto-generated catch block
			throw new RuntimeException("Error while trying to write REST response to file: " + ex.getMessage(), ex);
		}

		return filename;
	}

	protected static void setProxyAuthentication(final String proxyUsername, final String proxyPassword) {
		Authenticator authenticator = new Authenticator() {
			public PasswordAuthentication getPasswordAuthentication() {
				return (new PasswordAuthentication(proxyUsername, proxyPassword.toCharArray()));
			}
		};
		Authenticator.setDefault(authenticator);
	}
}
