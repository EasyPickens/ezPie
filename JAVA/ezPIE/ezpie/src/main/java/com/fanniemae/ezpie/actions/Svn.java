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

package com.fanniemae.ezpie.actions;

import java.io.File;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.common.ReportBuilder;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.common.XmlUtilities;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-05-24
 * 
 */

public class Svn extends RunCommand {

	protected String _batchFilename;

	public Svn(SessionManager session, Element action) {
		super(session, action, false);

		_workDirectory = requiredAttribute(action, "LocalPath").trim();
		if (FileUtilities.isInvalidDirectory(_workDirectory)) {
			File file = new File(_workDirectory);
			file.mkdirs();
		}
		_session.addLogMessage("", "Local Path", _workDirectory);

		// build command file
		NodeList nodeCmds = XmlUtilities.selectNodes(action, "*");
		int length = nodeCmds.getLength();
		if (length == 0) {
			throw new RuntimeException("No Svn actions defined.");
		}

		ReportBuilder sbCommands = new ReportBuilder();
		for (int i = 0; i < length; i++) {
			switch (nodeCmds.item(i).getNodeName()) {
			case "Clone":
				String repoURL = requiredAttribute(nodeCmds.item(i), "URL", "Svn Checkout requires a URL to remote SVN repository. Missing the URL.");
				String subDirectory = optionalAttribute(nodeCmds.item(i), "DirectoryName", null);

				String localPath = _workDirectory;
				if (StringUtilities.isNotNullOrEmpty(subDirectory)) {
					localPath = FileUtilities.addDirectory(_workDirectory, subDirectory);
				}
				
				if (FileUtilities.isInvalidDirectory(localPath)) {
					File file = new File(localPath);
					file.mkdirs();
					sbCommands.appendFormatLine("svn checkout %s %s", repoURL, StringUtilities.wrapValue(localPath));
				} else if (FileUtilities.isSvnRepository(localPath)) {
					sbCommands.appendFormatLine("svn update %s", localPath);
				} else if (FileUtilities.isNotEmptyDirectory(localPath)) {
					throw new PieException(String.format("Svn Checkout requires an empty destination directory. %s is not an empty directory.", localPath));
				} else {
					sbCommands.appendFormatLine("svn checkout %s %s", repoURL, StringUtilities.wrapValue(localPath));
				}
				break;
			case "Pull":
				sbCommands.appendLine("svn update");
				break;
			case "Reset":
				sbCommands.appendLine("svn reset -R");
				sbCommands.appendLine("svn status --no-ignore | grep -E '(^\\?)|(^\\I)' | sed -e 's/^. *//' | sed -e 's/\\(.*\\)/\"\\1\"/' | xargs rm -rf");
				sbCommands.appendLine("svn update --force");
				break;
			}
		}

		_session.addLogMessage("", "SVN Command", sbCommands.toString());
		_batchFilename = FileUtilities.writeRandomFile(_session.getStagingPath(), "bat", sbCommands.toString());
		_session.addLogMessage("", "Batch File", _batchFilename);
		_arguments = new String[] { _batchFilename };
	}
}
