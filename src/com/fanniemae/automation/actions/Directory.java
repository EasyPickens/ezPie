package com.fanniemae.automation.actions;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Element;

import com.fanniemae.automation.SessionManager;
import com.fanniemae.automation.common.DateUtilities;
import com.fanniemae.automation.common.FileUtilities;
import com.fanniemae.automation.common.ReportBuilder;
import com.fanniemae.automation.common.StringUtilities;

/**
 * 
 * @author Richard Monson
 * @since 2016-01-05
 * 
 */
public class Directory extends Action {
	protected String _Path;
	protected String _DestinationPath;
	protected String _NewName;

	// Variables used to profile directory
	protected String _SkippedExtensions;
	protected String _SkippedDirectories;
	protected String _LongestPath;
	protected String _LongestFilename;
	protected String _LargestFilename;

	// protected String[] _ExcludeExtensions;
	// protected String[] _ExcludeDirectories;

	protected Boolean _DeepScan = true;
	protected Boolean _SortByCount = true;
	protected Boolean _CheckSkipList = false;

	protected int _TotalFileCount = 0;
	protected int _TotalDirectoryCount = 0;
	protected int _ComponentCount = 0;
	protected int _LongestPathLength = 0;
	protected int _LongestFilenameLength = 0;

	protected long _TotalSize = 0L;
	protected long _LargestFileSize = 0L;

	protected Map<String, FileExtensionCount> _ExtensionCount = new HashMap<String, FileExtensionCount>();
	protected Map<String, String> _Components = new HashMap<String, String>();
	protected Map<String, Boolean> _ComponentExtensions = new HashMap<String, Boolean>();
	protected Map<String, Boolean> _SkipExtensions = new HashMap<String, Boolean>();
	protected Map<String, Boolean> _SkipDirectories = new HashMap<String, Boolean>();

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
				return deleteDirectory();
			case "Create":
				return createDirectory();
			case "Rename":
				return renameDirectory();
			case "Move":
				return moveDirectory();
			case "Profile":
				return profileDirectory();
			default:
				throw new IOException(String.format("%s is not currently supported.", _ActionType));
			}
		} catch (IOException ex) {
			throw new RuntimeException(String.format("%s could not %s %s.", _ActionName, _ActionType.toLowerCase(), _Path), ex);
		}
	}

	protected String RemoveFinalSlash(String path) {
		if (StringUtilities.isNotNullOrEmpty(path) && (path.endsWith(File.separator))) {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}

	protected String deleteDirectory() throws IOException {
		int iLevels = _Path.length() - _Path.replace(File.separator, "").length();
		if (iLevels == 0) {
			throw new RuntimeException(String.format("%s requires at least one directory level (%s).", _ActionName, _Path));
		}
		File fi = new File(_Path);
		if (!fi.exists()) {
			_Session.addLogMessage("", "Process", String.format("%s does not exist, nothing to delete.", _Path));
		} else if (fi.isFile()) {
			throw new IOException(String.format("%s is a file, use File.Delete to remove.", _Path));
		} else {
			_Session.addLogMessage("", "Process", String.format("Deleting %s", _Path));
			FileUtils.deleteDirectory(new File(_Path));
			_Session.addLogMessage("", "", "Completed");
		}
		return "";
	}

	protected String createDirectory() throws IOException {
		File fi = new File(_Path);
		if (!fi.exists()) {
			_Session.addLogMessage("", "Process", String.format("Creating %s", _Path));
			new File(_Path).mkdirs();
			_Session.addLogMessage("", "", "Completed");
		} else if (fi.isDirectory()) {
			_Session.addLogMessage("", "Process", "Directory already exists, nothing to do.");
		} else if (fi.isFile()) {
			throw new IOException(String.format("%s is the name of an existing file.", _Path));
		}
		return "";
	}

	protected String renameDirectory() throws IOException {
		File fi = new File(_Path);
		if (!fi.exists()) {
			throw new RuntimeException(String.format("Directory %s not found, nothing to rename.", _Path));
		} else if (fi.isDirectory()) {
			_Session.addLogMessage("", "Process", String.format("Renaming %s to %s.", _Path, _NewName));
			fi.renameTo(new File(_NewName));
			_Session.addLogMessage("", "", "Completed");
		} else if (fi.isFile()) {
			throw new RuntimeException(String.format("%s is a file.  Use the File.Rename operations to work with files.", _Path));
		}
		return "";
	}

	protected String moveDirectory() throws IOException {
		if (StringUtilities.isNullOrEmpty(_DestinationPath)) {
			throw new RuntimeException(String.format("%s is missing a value for DestinationPath.", _ActionName));
		}
		if (FileUtilities.isValidDirectory(_DestinationPath)) {
			throw new RuntimeException(String.format("Destination directory (%s) already exists.", _DestinationPath));
		}
		_Session.addLogMessage("", "Destination Path", _DestinationPath);
		_Session.addLogMessage("", "Process", "Moving directory");
		FileUtils.moveDirectoryToDirectory(new File(_Path), new File(_DestinationPath), true);
		return "";
	}

	protected String profileDirectory() {
		_ComponentExtensions.put("jar", true);
		_ComponentExtensions.put("dll", true);

		String sDeep = _Session.getAttribute(_Action, "Deep");
		if (StringUtilities.isNotNullOrEmpty(sDeep)) {
			_Session.addLogMessage("", "Deep", sDeep);
		}
		_DeepScan = StringUtilities.toBoolean(sDeep, true);

		_SkippedExtensions = _Session.getAttribute(_Action, "SkipExtensions");
		if (StringUtilities.isNotNullOrEmpty(_SkippedExtensions)) {
			_Session.addLogMessage("", "Skip Extensions", _SkippedExtensions);
			String[] aSkipExtensions = _SkippedExtensions.toLowerCase().split(",");
			if (aSkipExtensions.length > 0) {
				for (int i = 0; i < aSkipExtensions.length; i++) {
					_SkipExtensions.put(aSkipExtensions[i], true);
				}
				_CheckSkipList = true;
			}
		}

		_SkippedDirectories = _Session.getAttribute(_Action, "SkipDirectories");
		if (StringUtilities.isNotNullOrEmpty(_SkippedDirectories)) {
			_Session.addLogMessage("", "Skip Directories", _SkippedDirectories);
			String[] aSkipDirectories = _SkippedDirectories.toLowerCase().split(",");
			if (aSkipDirectories.length > 0) {
				for (int i = 0; i < aSkipDirectories.length; i++) {
					_SkipDirectories.put(aSkipDirectories[i], true);
				}
				_CheckSkipList = true;
			}
		}

		String sortByCount = _Session.getAttribute(_Action, "SortByCount");
		if (StringUtilities.isNotNullOrEmpty(sortByCount)) {
			_Session.addLogMessage("", "Sort by Count", sortByCount);
			_SortByCount = StringUtilities.toBoolean(sortByCount, true);
		}

		scanDirectory(_Path);

		Object[] aCounts = _ExtensionCount.values().toArray();
		Arrays.sort(aCounts);

		// Write the report
		ReportBuilder sbReport = new ReportBuilder();
		sbReport.appendFormatLine("Date Scanned: %1$s", DateUtilities.getCurrentDateTime());
		sbReport.appendFormatLine("Path: %1$s", _Path);
		if (_CheckSkipList && StringUtilities.isNotNullOrEmpty(_SkippedExtensions)) {
			sbReport.appendFormatLine("Skipped File Extensions: %1$s", _SkippedExtensions);
		}
		if (_CheckSkipList && StringUtilities.isNotNullOrEmpty(_SkippedDirectories)) {
			sbReport.appendFormatLine("Skipped Directories: %1$s", _SkippedDirectories);
		}
		sbReport.appendLine("");
		sbReport.appendFormatLine("Total File Count: %,d", _TotalFileCount);
		sbReport.appendFormatLine("Total Directory Count: %,d", _TotalDirectoryCount);
		sbReport.appendFormatLine("Component Files: %,d", _ComponentCount);
		sbReport.appendLine("");
		sbReport.appendFormatLine("Longest Path (%,d chars): %s", _LongestPathLength, _LongestPath);
		sbReport.appendFormatLine("Longest Filename (%,d chars): %s", _LongestFilenameLength, _LongestFilename);
		sbReport.appendLine("");
		sbReport.appendFormatLine("Largest File: (%,d bytes): %s", _LargestFileSize, _LargestFilename);
		sbReport.appendFormatLine("Total Space: %,d bytes", _TotalSize);
		sbReport.appendLine("");
		sbReport.appendLine("File Extensions (sorted by count): ");

		int iLen = aCounts.length;
		for (int i = 0; i < iLen; i++) {
			FileExtensionCount fileExtensionCount = (FileExtensionCount) aCounts[i];
			sbReport.appendFormatLine("   *.%1$s: %2$,d", fileExtensionCount.getExtension(), fileExtensionCount.getCount());
		}
		_Session.addLogMessage("", "Profile Results", "View Report", "file://" + FileUtilities.writeRandomTextFile(_Session.getLogPath(), sbReport.toString()));
		return "";
	}

	protected void scanDirectory(String path) {
		File[] aContents = new File(path).listFiles();
		if ((aContents == null) || (aContents.length == 0))
			return;

		if (path.length() > _LongestPathLength) {
			_LongestPathLength = path.length();
			_LongestPath = path;
		}

		for (File currentFile : aContents) {
			if (currentFile.isDirectory() && _DeepScan) {
				String subPath = currentFile.getAbsolutePath();
				subPath = subPath.substring(subPath.lastIndexOf(File.separatorChar)).toLowerCase();
				if (_CheckSkipList && _SkipDirectories.containsKey(subPath)) {
					continue;
				}
				_TotalDirectoryCount++;
				scanDirectory(currentFile.getAbsolutePath());
				continue;
			}

			String sExtension = FilenameUtils.getExtension(currentFile.getName()).toLowerCase();
			if (_CheckSkipList && _SkipExtensions.containsKey(sExtension)) {
				continue;
			}

			_TotalFileCount++;
			_TotalSize += currentFile.length();

			if (currentFile.getName().length() > _LongestFilenameLength) {
				_LongestFilenameLength = currentFile.getName().length();
				_LongestFilename = currentFile.getName();
			}

			if (currentFile.length() > _LargestFileSize) {
				_LargestFileSize = currentFile.length();
				_LargestFilename = currentFile.getName();
			}

			if (_ComponentExtensions.containsKey(sExtension)) {
				_ComponentCount++;
			}

			FileExtensionCount cnt = _ExtensionCount.get(sExtension);
			if (cnt == null) {
				_ExtensionCount.put(sExtension, new FileExtensionCount(sExtension, _SortByCount));
			} else {
				cnt.increment();
			}
		}
	}

	protected class FileExtensionCount implements Comparable<FileExtensionCount> {
		protected Boolean _SortByCount = true;
		protected String _Extension;
		protected int _Count = 1;

		public FileExtensionCount(String extension) {
			this(extension, true);
		}

		public FileExtensionCount(String extension, Boolean sortByCount) {
			if (extension == null)
				extension = "";
			_Extension = extension;
			_SortByCount = sortByCount;
		}

		public String getExtension() {
			if (_Extension.length() == 0)
				return "(blank)";
			return _Extension;
		}

		public void increment() {
			++_Count;
		}

		public int getCount() {
			return _Count;
		}

		@Override
		public int compareTo(FileExtensionCount o) {
			if (_SortByCount) {
				return o._Count - this._Count;
			} else {
				return this._Extension.compareTo(o._Extension);
			}
		}
	}

}
