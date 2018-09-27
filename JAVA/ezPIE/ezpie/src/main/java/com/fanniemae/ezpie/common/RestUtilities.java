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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.DatatypeConverter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 *
 * @author Tara Tritt
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2016-05-30
 * 
 */

public final class RestUtilities {

	private RestUtilities() {
	}

	public static String sendGetRequest(String urlStr) {
		return sendGetRequest(urlStr, null, 0, null, null, null, null, null);
	}

	public static String sendGetRequest(String urlStr, String username, String password) {
		return sendGetRequest(urlStr, null, 0, null, null, username, password, null);
	}

	public static String sendGetRequest(String urlStr, String proxyHost, int proxyPort, String proxyUsername, String proxyPassword) {
		return sendGetRequest(urlStr, proxyHost, proxyPort, proxyUsername, proxyPassword, null, null, null);
	}

	public static String sendGetRequest(String urlStr, String proxyHost, int proxyPort, String proxyUsername, String proxyPassword, String username, String password, Map<String,String> header) {
		return sendRequest(false, urlStr, null, proxyHost, proxyPort, proxyUsername, proxyPassword, username, password, header);
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
		return sendRequest(true, urlStr, body, proxyHost, proxyPort, proxyUsername, proxyPassword, username, password, null);
	}

	public static String sendRequest(boolean post, String urlStr, String body, String proxyHost, int proxyPort, String proxyUsername, String proxyPassword, String username, String password, Map<String,String> header) {

		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {
				// Trust all certificates -- for self signed certs.
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
				// Trust all certificates -- for self signed certs.
			}

		} };

		SSLContext sc = null;
		try {
			sc = SSLContext.getInstance("SSL");
		} catch (NoSuchAlgorithmException e) {
			throw new PieException(e);
		}
		try {
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
		} catch (KeyManagementException e) {
			throw new PieException(e);
		}
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		// Create all-trusting host name verifier
		HostnameVerifier allHostsValid = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};
		// Install the all-trusting host verifier
		HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

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
				if ((header != null) && (header.size() > 0)) {
					for (Map.Entry<String,String> entry : header.entrySet()) {
						connection.setRequestProperty(entry.getKey(), entry.getValue());
					}
				}
			}
			if (post) {
				connection.setRequestProperty("Content-Type", "application/json");
				if ((header != null) && (header.size() > 0)) {
					for (Map.Entry<String,String> entry : header.entrySet()) {
						connection.setRequestProperty(entry.getKey(), entry.getValue());
					}
				}				
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

			try {
				// Pretty print if JSON object or array.
				if ((responseStr != null) && (responseStr.charAt(0) == '[')) {
					JSONArray jarray = new JSONArray(responseStr);
					responseStr = jarray.toString(4);
				} else if (responseStr != null) {
					JSONObject jobject = new JSONObject(responseStr);
					responseStr = jobject.toString(4);
				}
			} catch (Exception exx) {
				ExceptionUtilities.goSilent(exx);
			}

			return responseStr;
		} catch (JSONException | IOException ex) {
			throw new PieException("Error while trying to make REST request: " + ex.getMessage(), ex);
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
			throw new PieException("Error while trying to write REST response to file: " + ex.getMessage(), ex);
		}

		return filename;
	}

	public static String sendRequest(RestRequestConfiguration rrc) {

		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {
				// Trust all certificates -- for self signed certs.
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
				// Trust all certificates -- for self signed certs.
			}

		} };

		SSLContext sc = null;
		try {
			sc = SSLContext.getInstance("SSL");
		} catch (NoSuchAlgorithmException e) {
			throw new PieException(e);
		}
		try {
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
		} catch (KeyManagementException e) {
			throw new PieException(e);
		}
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		// Create all-trusting host name verifier
		HostnameVerifier allHostsValid = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};
		// Install the all-trusting host verifier
		HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

		try {
			URL url = new URL(rrc.getUrl());

			HttpURLConnection connection;
			if (StringUtilities.isNotNullOrEmpty(rrc.getProxyHost())) {
				Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(rrc.getProxyHost(), rrc.getProxyPort()));
				connection = (HttpURLConnection) url.openConnection(proxy);
				setProxyAuthentication(rrc.getProxyUsername(), rrc.getProxyPassword());
			} else {
				connection = (HttpURLConnection) url.openConnection();
			}

			Map<String, String> properties = rrc.getRequestProperties();
			if ((properties != null) && !properties.isEmpty()) {
				for (Map.Entry<String, String> entry : properties.entrySet()) {
					connection.setRequestProperty(entry.getKey(), entry.getValue());
				}
			}

			if ("POST".equals(rrc.getRequestMethod())) {
				connection.setRequestMethod(rrc.getRequestMethod());
				connection.setDoOutput(true);
				DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
				wr.writeBytes(rrc.getMessageBody());
				wr.flush();
				wr.close();
			} else if ("GET".equals(rrc.getRequestMethod())) {
				connection.setRequestMethod("GET");
			} else {
				throw new RuntimeException(String.format("The REST connector does not currently support the %s request method.", rrc.getRequestMethod()));
			}

			String responseStr;
			try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				String inputLine;
				StringBuffer responseBuffer = new StringBuffer();
				while ((inputLine = br.readLine()) != null) {
					responseBuffer.append(inputLine);
				}
				responseStr = responseBuffer.toString();
			}

			return responseStr;
		} catch (JSONException | IOException ex) {
			throw new PieException("Error while trying to make REST request: " + ex.getMessage(), ex);
		}
	}

	protected static void setProxyAuthentication(final String proxyUsername, final String proxyPassword) {
		Authenticator authenticator = new Authenticator() {
			public PasswordAuthentication getPasswordAuthentication() {
				return (new PasswordAuthentication(proxyUsername, proxyPassword.toCharArray()));
			}
		};
		Authenticator.setDefault(authenticator);
	}

	protected static void clearProxyAuthentication() {
		Authenticator authenticator = new Authenticator() {
			public PasswordAuthentication getPasswordAuthentication() {
				return (new PasswordAuthentication("", "".toCharArray()));
			}
		};
		Authenticator.setDefault(authenticator);
	}
}
