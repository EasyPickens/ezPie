/**
 *  
 * Copyright (c) 2017 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 * 
**/

package com.fanniemae.ezpie.common;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.FS;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import sun.net.www.protocol.http.AuthCacheImpl;
import sun.net.www.protocol.http.AuthCacheValue;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2017-07-18
 * 
 */

@SuppressWarnings("restriction")
public class GitOperations {

	protected boolean _useProxy = false;
	protected boolean _proxyRequiresAuthentication = false;

	protected String _proxyHost;
	protected int _proxyPort;
	protected String _proxyUserID;
	protected String _proxyPassword;

	protected String _repositoryHost;

	public GitOperations() {
	}

	public GitOperations(String proxyHost, String proxyPort, String proxyUserID, String proxyPassword) {
		_proxyHost = proxyHost;
		_proxyPort = StringUtilities.toInteger(proxyPort, 80);
		_proxyUserID = proxyUserID;
		_proxyPassword = proxyPassword;

		_useProxy = StringUtilities.isNotNullOrEmpty(_proxyHost);
		_proxyRequiresAuthentication = StringUtilities.isNotNullOrEmpty(_proxyUserID) || StringUtilities.isNotNullOrEmpty(_proxyPassword);
	}

	public String cloneHTTP(String repo_url, String destination) throws InvalidRemoteException, TransportException, GitAPIException, URISyntaxException {
		return cloneHTTP(repo_url, destination, null, null, null);
	}

	public String cloneHTTP(String repo_url, String destination, String userID, String password, String branch) throws InvalidRemoteException, TransportException, GitAPIException, URISyntaxException {

		_repositoryHost = getHost(repo_url);
		setupProxy();

		File localDir = new File(destination);
		CloneCommand cloneCommand = Git.cloneRepository();
		cloneCommand.setURI(repo_url);
		cloneCommand.setDirectory(localDir);

		if (StringUtilities.isNotNullOrEmpty(branch))
			cloneCommand.setBranch(branch);

		if (StringUtilities.isNotNullOrEmpty(userID))
			cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(userID, password));

		try (Writer writer = new StringWriter()) {
			TextProgressMonitor tpm = new TextProgressMonitor(writer);
			cloneCommand.setProgressMonitor(tpm);
			try (Git result = cloneCommand.call()) {
			}
			clearProxyAuthenticatorCache();
			writer.flush();
			return writer.toString();
		} catch (IOException e) {
			throw new PieException("Error while trying to clone the git repository. " + e.getMessage(), e);
		}
	}

	public String cloneSSH(String repo_url, String destination, String privateKey, String password, String branch) {
		if (_useProxy) {
			throw new PieException("Network proxies do not support SSH, please use an http url to clone this repository.");
		}

		final SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
			@Override
			protected void configure(Host host, Session session) {
				// This still checks existing host keys and will disable "unsafe"
				// authentication mechanisms if the host key doesn't match.
				session.setConfig("StrictHostKeyChecking", "no");
			}

			@Override
			protected JSch createDefaultJSch(FS fs) throws JSchException {
				JSch defaultJSch = super.createDefaultJSch(fs);
				defaultJSch.addIdentity(privateKey, password);
				return defaultJSch;
			}
		};

		CloneCommand cloneCommand = Git.cloneRepository();
		cloneCommand.setURI(repo_url);
		File dir = new File(destination);
		cloneCommand.setDirectory(dir);

		if (StringUtilities.isNotNullOrEmpty(branch))
			cloneCommand.setBranch(branch);

		cloneCommand.setTransportConfigCallback(new TransportConfigCallback() {
			public void configure(Transport transport) {
				SshTransport sshTransport = (SshTransport) transport;
				sshTransport.setSshSessionFactory(sshSessionFactory);
			}
		});

		try (Writer writer = new StringWriter()) {
			TextProgressMonitor tpm = new TextProgressMonitor(writer);
			cloneCommand.setProgressMonitor(tpm);
			try (Git result = cloneCommand.call()) {
			}
			writer.flush();
			return writer.toString();
		} catch (IOException | GitAPIException e) {
			throw new PieException("Error while trying to clone the git repository. " + e.getMessage(), e);
		}
	}

	private void setupProxy() {
		if (!_useProxy) {
			// Repository does not require a proxy, so clear any proxy information from the environement.
			AuthCacheValue.setAuthCache(new AuthCacheImpl());
			ProxySelector.setDefault(new ProxySelector() {
				
				@Override
				public List<Proxy> select(URI uri) {
					return Arrays.asList(Proxy.NO_PROXY);
				}

				@Override
				public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
					if (uri == null || sa == null || ioe == null) {
						throw new IllegalArgumentException("Arguments can't be null.");
					}
				}
			});
			
			return;
		}

		if (_proxyRequiresAuthentication) {
			setupProxyAuthentication();
		}

		ProxySelector.setDefault(new ProxySelector() {
			final ProxySelector delegate = ProxySelector.getDefault();

			@Override
			public List<Proxy> select(URI uri) {
				// Filter the URIs to be proxied
				if (uri.toString().contains("github") && uri.toString().contains("https")) {
					return Arrays.asList(new Proxy(Type.HTTP, InetSocketAddress.createUnresolved(_proxyHost, _proxyPort)));
				}
				if (uri.toString().contains("github") && uri.toString().contains("http")) {
					return Arrays.asList(new Proxy(Type.HTTP, InetSocketAddress.createUnresolved(_proxyHost, _proxyPort)));
				}
				// revert to the default behaviour
				return delegate == null ? Arrays.asList(Proxy.NO_PROXY) : delegate.select(uri);
			}

			@Override
			public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
				if (uri == null || sa == null || ioe == null) {
					throw new IllegalArgumentException("Arguments can't be null.");
				}
			}
		});
	}

	private void clearProxyAuthenticatorCache() {
		if (_proxyRequiresAuthentication) {
			AuthCacheValue.setAuthCache(new AuthCacheImpl());
		}
	}

	private void setupProxyAuthentication() {
		Authenticator.setDefault(new Authenticator() {
			@Override
			public PasswordAuthentication getPasswordAuthentication() {
				// If proxy is non authenticated for some URLs, the requested URL is the endpoint (and not the proxy host)
				// In this case the authentication should not be the one of proxy ... so return null (and JGit CredentialsProvider will be used)
				if (super.getRequestingHost().equals(_proxyHost)) {
					char[] password = StringUtilities.isNotNullOrEmpty(_proxyPassword) ? _proxyPassword.toCharArray() : null;
					return new PasswordAuthentication(_proxyUserID, password);
				}
				return null;
			}
		});
	}

	private String getHost(String url) throws URISyntaxException {
		URI uri = new URI(url);
		return uri.getHost();
	}
}
