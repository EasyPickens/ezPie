package com.fanniemae.devtools.pie.actions;

import java.io.File;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.FileUtilities;
import com.fanniemae.devtools.pie.common.ReportBuilder;
import com.fanniemae.devtools.pie.common.StringUtilities;
import com.fanniemae.devtools.pie.common.XmlUtilities;

/**
 * 
 * @author Richard Monson
 * @since 2016-01-04
 * 
 * <Svn LocalPath="">
 *    <Clone URL="" />
 *    <Pull />
 *    <Reset />
 * </Svn>
 */

public class Svn extends RunCommand {

	protected String _batchFilename;

	public Svn(SessionManager session, Element action) {
		super(session, action, false);

		_workDirectory = _session.getAttribute(action, "LocalPath").trim();
		if (StringUtilities.isNullOrEmpty(_workDirectory)) {
			throw new RuntimeException("No LocalPath specified for Svn action.");
		} else if (FileUtilities.isInvalidDirectory(_workDirectory)) {
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
				String repoURL = _session.getAttribute(nodeCmds.item(i), "URL");
				String subDirectory = _session.getAttribute(nodeCmds.item(i), "DirectoryName");
				if (StringUtilities.isNullOrEmpty(repoURL)) {
					throw new RuntimeException("Svn Checkout requires a URL to remote SVN repository. Missing the URL.");
				}
				String localPath = _workDirectory;
				if (StringUtilities.isNullOrEmpty(subDirectory)) {
					int iPos = repoURL.lastIndexOf('/');
					if ((iPos > -1) && (iPos < repoURL.length() - 1)) {
						subDirectory = repoURL.substring(iPos + 1);
					} else {
						throw new RuntimeException(String.format("Could not parse SVN repository URL (%s) for directory name.", repoURL));
					}
				}
				localPath = FileUtilities.addDirectory(_workDirectory, subDirectory);
				if (FileUtilities.isInvalidDirectory(localPath)) {
					File file = new File(localPath);
					file.mkdirs();
				} else if (FileUtilities.isSvnRepository(localPath)) {
					sbCommands.appendFormatLine("svn update %s", localPath);
				} else if (FileUtilities.isNotEmptyDirectory(localPath)) {
					throw new RuntimeException(String.format("Svn Checkout requires an empty destination directory. %s is not an empty directory.", localPath));
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
