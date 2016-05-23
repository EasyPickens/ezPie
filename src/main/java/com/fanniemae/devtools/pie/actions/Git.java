package com.fanniemae.devtools.pie.actions;

import java.io.File;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.FileUtilities;
import com.fanniemae.devtools.pie.common.ReportBuilder;
import com.fanniemae.devtools.pie.common.StringUtilities;
import com.fanniemae.devtools.pie.common.XmlUtilities;

///   <Git LocalPath="" PLinkPath="">
///      <Clone RemoteRepository="" />
///      <Commit Message=""/>  git commit -m "message"
///      <Push RemoteRepo="" RemoteBranch="" IncludeTags="" /> git push <remote name> <branch name> --tags
///      <Pull /> git pull --rebase
///      <Checkout Branch="" Tag="" />
///      <Reset Hard="True/False" />
///      <AddTag Tag="" Message="" /> git tag -a v1.4 -m "message"
///   </Git>

public class Git extends RunCommand {

	protected String _localPath;
	protected String _plinkPath;
	protected String _batchFilename;

	public Git(SessionManager session, Element action) {
		super(session, action, false);

		_localPath = _session.getAttribute(action, "LocalPath").trim();
		if (StringUtilities.isNullOrEmpty(_localPath)) {
			throw new RuntimeException("No LocalPath specified for Git actions.");
		} else if (FileUtilities.isInvalidDirectory(_localPath)) {
			File file = new File(_localPath);
			file.mkdirs();
		}
		_workDirectory = _localPath;

		// Using public/private keys requires a key store.
		_plinkPath = _session.getAttribute(action, "PLinkPath").trim();
		if (StringUtilities.isNotNullOrEmpty(_plinkPath) && FileUtilities.isInvalidFile(_plinkPath)) {
			throw new RuntimeException(String.format("Plink.exe file not found for PLinkPath %s.", _plinkPath));
		}

		// build command file
		NodeList nodeCmds = XmlUtilities.selectNodes(action, "*");
		int length = nodeCmds.getLength();
		if (length == 0) {
			throw new RuntimeException("No Git actions defined.");
		}

		//String message;
		String tag;
		String remoteRepo;
		ReportBuilder sbCommands = new ReportBuilder();
		if (StringUtilities.isNotNullOrEmpty(_plinkPath)) {
			sbCommands.appendFormatLine("SET GIT_SSH=%s", _plinkPath);
		}
		for (int i = 0; i < length; i++) {
			switch (nodeCmds.item(i).getNodeName()) {
			case "Clone":
				remoteRepo = _session.getAttribute(nodeCmds.item(i), "RemoteRepository");
				if (StringUtilities.isNullOrEmpty(remoteRepo)) {
					throw new RuntimeException("Missing RemoteRepository information.");
				}
				if (FileUtilities.isNotEmptyDirectory(_localPath) && FileUtilities.isGitRepository(_localPath)) {
					sbCommands.appendLine("git clean -df");
					sbCommands.appendLine("git reset --hard");
					sbCommands.appendLine("git pull --rebase");
				} else if (FileUtilities.isNotEmptyDirectory(_localPath)) {
					throw new RuntimeException(String.format("Git clone requires an empty destination directory. %s is not an empty directory.", _localPath));
				} else {
				sbCommands.appendFormatLine("git clone --verbose %s %s", StringUtilities.wrapValue(remoteRepo), StringUtilities.wrapValue(_localPath));
				}				
				break;
			case "Commit":
//				message = _session.getAttribute(nodeCmds.item(i), "Message");
//				if (StringUtilities.isNullOrEmpty(message)) {
//					throw new RuntimeException("No commit message defined.");
//				}
//				sbCommands.appendFormatLine("git commit -m  \"%s\"", message);
//				break;				
				throw new RuntimeException("Commit support not currently available.");
			case "Push":
//				remoteRepo = _session.getAttribute(nodeCmds.item(i), "RemoteRepository");
				throw new RuntimeException("Push support not currently available.");
			case "Pull":
				sbCommands.appendLine("git pull --rebase");
				break;
			case "Checkout":
				String branch = _session.getAttribute(nodeCmds.item(i), "Branch");
				if (StringUtilities.isNullOrEmpty(branch)) {
					throw new RuntimeException("Missing required branch name to checkout.");
				}
				tag = _session.getAttribute(nodeCmds.item(1), "Tag");
				sbCommands.appendFormat("git checkout %s %s", StringUtilities.wrapValue(branch), tag);
				break;
			case "Reset":
				String hard = _session.getAttribute(nodeCmds.item(i), "Hard").toLowerCase().equals("true") ? "--hard" : "";
				sbCommands.appendFormatLine("git reset %s", hard);
				break;
			case "AddTag":
//				tag = _session.getAttribute(nodeCmds.item(i), "Tag");
//				if (StringUtilities.isNullOrEmpty(tag)) {
//					throw new RuntimeException("No Tag value defined.");
//				}
//
//				message = _session.getAttribute(nodeCmds.item(i), "Message");
//				if (StringUtilities.isNullOrEmpty(message)) {
//					throw new RuntimeException("Missing Message associated with this tag.");
//				}
//				sbCommands.appendFormatLine("git tag -a %s -m \"%s\"", tag, message);
//				break;
				throw new RuntimeException("AddTag support not currently available.");				
			}
		}
		
		_session.addLogMessage("", "Git Commands", sbCommands.toString());
		_batchFilename = FileUtilities.writeRandomFile(_session.getStagingPath(), "bat", sbCommands.toString());
		_session.addLogMessage("", "Batch File", _batchFilename);
		_arguments = new String[] {_batchFilename};
	}

	@Override
	public String execute() {
		super.execute();
		return "";
	}

}
