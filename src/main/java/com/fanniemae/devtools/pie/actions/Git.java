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
 * @since 2016-05-19
 * 
 * <Git LocalPath="" [PLinkPath]>
 *    <Clone URL="" />
 *    <Pull />
 *    <Checkout Branch="" [Tag] />
 *    <Reset [Hard] />
 * </Git>
 */

public class Git extends RunCommand {

	protected String _plinkPath;
	protected String _batchFilename;

	public Git(SessionManager session, Element action) {
		super(session, action, false);

		_workDirectory = _session.getAttribute(action, "LocalPath").trim();
		if (StringUtilities.isNullOrEmpty(_workDirectory)) {
			throw new RuntimeException("No LocalPath specified for Git action.");
		} else if (FileUtilities.isInvalidDirectory(_workDirectory)) {
			File file = new File(_workDirectory);
			file.mkdirs();
		}
		_session.addLogMessage("", "Local Path", _workDirectory);

		// Using public/private keys requires a key store.
		_plinkPath = _session.getAttribute(action, "PLinkPath").trim();
		if (StringUtilities.isNullOrEmpty(_plinkPath)) {
			_plinkPath=_session.resolveTokens("@Git.PLinkPath~").trim();
		}
		_session.addLogMessage("", "PLink Path", _plinkPath);
		
		if (StringUtilities.isNotNullOrEmpty(_plinkPath) && FileUtilities.isInvalidFile(_plinkPath)) {
			throw new RuntimeException(String.format("Plink.exe file not found for PLinkPath %s.", _plinkPath));
		}

		// build command file
		NodeList nodeCmds = XmlUtilities.selectNodes(action, "*");
		int length = nodeCmds.getLength();
		if (length == 0) {
			if (FileUtilities.isEmptyDirectory(_workDirectory)) {
				throw new RuntimeException("No Git actions defined.");
			}
			// default to a reset and pull.
			Element reset = action.getOwnerDocument().createElement("Reset");
			reset.setAttribute("Hard", "True");
			action.appendChild(reset);
			action.appendChild(action.getOwnerDocument().createElement("Pull"));
			nodeCmds = XmlUtilities.selectNodes(action, "*");
			length = nodeCmds.getLength();
		}

		// String message;
		String tag;
		String repoURL;
		ReportBuilder sbCommands = new ReportBuilder();
		if (StringUtilities.isNotNullOrEmpty(_plinkPath)) {
			sbCommands.appendFormatLine("SET GIT_SSH=%s", _plinkPath);
		} else {
			_session.addLogMessage("", "**NOTE", "If using SSH and seeing errors, you may need to set PLinkPath in the settings file.  It should be the full path to the plink.exe file.");
		}
		for (int i = 0; i < length; i++) {
			switch (nodeCmds.item(i).getNodeName()) {
			case "Clone":
				repoURL = _session.getAttribute(nodeCmds.item(i), "URL");
				if (StringUtilities.isNullOrEmpty(repoURL)) {
					throw new RuntimeException("Missing URL to remote repository.");
				}
				if (FileUtilities.isNotEmptyDirectory(_workDirectory) && FileUtilities.isGitRepository(_workDirectory)) {
					sbCommands.appendLine("git clean -df");
					sbCommands.appendLine("git reset --hard");
					sbCommands.appendLine("git pull --rebase");
				} else if (FileUtilities.isNotEmptyDirectory(_workDirectory)) {
					throw new RuntimeException(String.format("Git clone requires an empty destination directory. %s is not an empty directory.", _workDirectory));
				} else {
					sbCommands.appendFormatLine("git clone --verbose %s %s", StringUtilities.wrapValue(repoURL), StringUtilities.wrapValue(_workDirectory));
				}
				break;
			case "Pull":
				sbCommands.appendLine("git pull --rebase");
				break;
			case "Checkout":
				String branch = _session.getAttribute(nodeCmds.item(i), "Branch");
				if (StringUtilities.isNullOrEmpty(branch)) {
					throw new RuntimeException("Missing required branch name to checkout.");
				}
				tag = _session.getAttribute(nodeCmds.item(1), "Tag");
				sbCommands.appendFormatLine("git checkout %s %s", StringUtilities.wrapValue(branch), tag);
				break;
			case "Reset":
				String hard = _session.getAttribute(nodeCmds.item(i), "Hard").toLowerCase().equals("true") ? "--hard" : "";
				sbCommands.appendLine("git clean -df");
				sbCommands.appendFormatLine("git reset %s", hard);
				break;
			}
		}

		_session.addLogMessage("", "Git Commands", sbCommands.toString());
		_batchFilename = FileUtilities.writeRandomFile(_session.getStagingPath(), "bat", sbCommands.toString());
		_session.addLogMessage("", "Batch File", _batchFilename);
		_arguments = new String[] { _batchFilename };
	}
}
