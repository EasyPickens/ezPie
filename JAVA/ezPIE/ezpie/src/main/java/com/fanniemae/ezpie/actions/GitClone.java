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

package com.fanniemae.ezpie.actions;

import java.io.File;
import java.util.HashMap;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.GitUtilities;
import com.fanniemae.ezpie.common.StringUtilities;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2017-07-18
 * 
 */

public class GitClone extends Action {

	public GitClone(SessionManager session, Element action) {
		super(session, action, false);
	}

	@Override
	public String executeAction(HashMap<String, String> dataTokens) {
		// Repository connection information could be on the local element or stored in the settings file.
		// Check the local element first, if nothing is found and a connection name is provided switch to the settings file.

		String repo_uri = optionalAttribute("URL");
		String branch = optionalAttribute("Branch");
		String localPath = requiredAttribute("LocalPath").trim();
		String repo_userID = optionalAttribute("UserID");
		String repo_password = optionalAttribute("Password");
		String privateKey = optionalAttribute("PrivateKey");
		String connectionName = optionalAttribute("ConnectionName");
		if (StringUtilities.isNotNullOrEmpty(connectionName)) {
			Element repo_connection = _session.getConnection(connectionName);
			repo_uri = StringUtilities.isNullOrEmpty(repo_uri) ? optionalAttribute(repo_connection, "URL", repo_uri) : repo_uri;
			repo_userID = StringUtilities.isNullOrEmpty(repo_userID) ? optionalAttribute(repo_connection, "UserID", repo_userID) : repo_userID;
			repo_password = StringUtilities.isNullOrEmpty(repo_password) ? optionalAttribute(repo_connection, "Password", repo_password) : repo_password;
			privateKey = StringUtilities.isNullOrEmpty(privateKey) ? optionalAttribute(repo_connection, "PrivateKey", privateKey) : privateKey;
		}

		if (StringUtilities.isNotNullOrEmpty(privateKey) && FileUtilities.isInvalidFile(privateKey)) {
			throw new RuntimeException(String.format("Private key file %s was not found.", privateKey));
		}
		
		if (FileUtilities.isInvalidDirectory(localPath)) {
			File file = new File(localPath);
			file.mkdirs();
		}

		try {
			String log = "No Information Available.";
			if (StringUtilities.isNullOrEmpty(privateKey)) {
				log = GitUtilities.cloneHTTP(repo_uri, localPath, repo_userID, repo_password, branch);
			} else {
				log = GitUtilities.cloneSSH(repo_uri, localPath, privateKey, repo_password, branch);
			}
			String filename = FileUtilities.writeRandomTextFile(_session.getLogPath(), log);
			_session.addLogMessage("", "Clone Output", "View Clone Log", "file://" + filename);
		} catch (GitAPIException e) {
			throw new RuntimeException(String.format("Error while trying to clone %s over HTTP connection. %s",repo_uri, e),e);
		}
		
		return null;
	}

}
