/**
 *  
 * Copyright (c) 2017 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
**/

package com.fanniemae.ezpie.common;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

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

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2017-07-18
 * 
 */

public class GitUtilities {

	private GitUtilities() {
	}

	public static String cloneHTTP(String repo_url, String destination) throws InvalidRemoteException, TransportException, GitAPIException {
		return cloneHTTP(repo_url, destination, null, null, null);
	}

	public static String cloneHTTP(String repo_url, String destination, String userID, String password, String branch) throws InvalidRemoteException, TransportException, GitAPIException {
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
			writer.flush();
			return writer.toString();
		} catch (IOException e) {
			throw new RuntimeException("Error while trying to clone the git repository. " + e.getMessage(), e);
		}
	}

	public static String cloneSSH(String repo_url, String destination, String privateKey, String password, String branch) {

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
			throw new RuntimeException("Error while trying to clone the git repository. " + e.getMessage(), e);
		}
	}
}
