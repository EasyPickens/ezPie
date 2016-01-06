package com.fanniemae.automation.actions;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Element;

import com.fanniemae.automation.SessionManager;
import com.fanniemae.automation.common.FileUtilities;
import com.fanniemae.automation.common.StringUtilities;

public class Directory extends Action {
	protected String _Path;
	protected String _DestinationPath;
	protected String _NewName;

	public Directory(SessionManager session, Element eleAction) {
		super(session, eleAction, false);

		_Path = RemoveFinalSlash(_Session.getAttribute(_Action, "Path"));
		_DestinationPath = RemoveFinalSlash(_Session.getAttribute(_Action, "DestinationPath"));
		_NewName = _Session.getAttribute(_Action, "NewName");

		if (StringUtilities.isNullOrEmpty(_Path)) {
			throw new RuntimeException(String.format("%s is missing a value for Path.", _ActionName));
		}
		_Session.addLogMessage("", "Path", _Path);

	}

	@Override
	public String execute() {
		try {
			switch (_ActionType) {
			case "Delete":
				int iLevels = _Path.length() - _Path.replace(File.separator, "").length();
				if (iLevels == 0) {
					throw new RuntimeException(String.format("%s requires at least one directory level (%s).", _ActionName, _Path));
				}
				_Session.addLogMessage("", "Process", "Deleting " + _Path);
				FileUtils.deleteDirectory(new File(_Path));
				break;
			case "Create":
				_Session.addLogMessage("", "Process", "Creating " + _Path);
				new File(_Path).mkdirs();
				break;
			case "Rename":
				_Session.addLogMessage("", "Process", String.format("Renaming %s to %s.", _Path, _NewName));
				File fi = new File(_Path);
				if (fi.isDirectory()) {
					fi.renameTo(new File(_NewName));
				} else if (fi.isFile()) {
					throw new RuntimeException(String.format("%s is a file.  Use the File.Rename operations to work with files.", _Path));
				} else {
					throw new RuntimeException(String.format("Directory %s not found - nothing to rename.", _Path));
				}
				break;
			case "Move":
				if (StringUtilities.isNullOrEmpty(_DestinationPath)) {
					throw new RuntimeException(String.format("%s is missing a value for DestinationPath.", _ActionName));
				}
				_Session.addLogMessage("", "Destination Path", _DestinationPath);
				_Session.addLogMessage("", "Process", "Moving directory");
				FileUtils.moveDirectoryToDirectory(new File(_Path), new File(_DestinationPath), true);
				break;
			default:
				throw new IOException(String.format("%s is not currently supported.", _ActionType));
			}
			_Session.addLogMessage("", "", "Completed");
		} catch (IOException ex) {
			throw new RuntimeException(String.format("%s could not %s %s.", _ActionName, _ActionType.toLowerCase(), _Path), ex);
		}
		return null;
	}

	protected String RemoveFinalSlash(String path) {
		if (StringUtilities.isNotNullOrEmpty(path) && (path.endsWith(File.separator))) {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}

}
