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
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 *
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2018-01-17 (base 2016-05-30)
 * 
 */

public class RestUtilitiesV2 {

	private RestUtilitiesV2() {
	}

	public static String sendRequest(RestRequestConfiguration rrc) {
		SSLSocketFactory originalFactory = null;
		String responseStr;

		try {
			if (!rrc.getValidateCerfificate()) {
				originalFactory = trustAllCertificates();
			}

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

			try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				String inputLine;
				StringBuffer responseBuffer = new StringBuffer();
				while ((inputLine = br.readLine()) != null) {
					responseBuffer.append(inputLine);
				}
				responseStr = responseBuffer.toString();
			}
		} catch (IOException | KeyManagementException | NoSuchAlgorithmException ex) {
			throw new PieException("Error while trying to make REST request: " + ex.getMessage(), ex);
		}
		if (originalFactory != null) {
			HttpsURLConnection.setDefaultSSLSocketFactory(originalFactory);
		}
		return responseStr;
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

	protected static void skipCertificateValidation() {
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {
				// Trust all certificates -- typically used for self signed certs.
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
				// Trust all certificates -- typically used for self signed certs.
			}

		} };

		SSLContext sc = null;
		try {
			sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
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
	}

	private static SSLSocketFactory trustAllCertificates() throws NoSuchAlgorithmException, KeyManagementException {
		SSLContext sslContext = SSLContext.getInstance("SSL");
		sslContext.init(null, new TrustManager[] { ACCEPT_ALL_TRUSTMANAGER }, null);
		SSLSocketFactory originalFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
		HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
		return originalFactory;
	}

	private static final TrustManager ACCEPT_ALL_TRUSTMANAGER = new X509TrustManager() {
		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		@Override
		public void checkClientTrusted(X509Certificate[] certs, String authType) {
			// do nothing
		}

		@Override
		public void checkServerTrusted(X509Certificate[] certs, String authType) {
			// do nothing
		}
	};
}
