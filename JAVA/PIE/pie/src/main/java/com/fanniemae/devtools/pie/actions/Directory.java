package com.fanniemae.devtools.pie.actions;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.DateUtilities;
import com.fanniemae.devtools.pie.common.FileUtilities;
import com.fanniemae.devtools.pie.common.ReportBuilder;
import com.fanniemae.devtools.pie.common.StringUtilities;

/**
 * 
 * @author Richard Monson
 * @since 2016-01-05
 * 
 */
public class Directory extends Action {
	protected String _path;
	protected String _destinationPath;
	protected String _newName;

	// Variables used to profile directory
	protected String _skippedExtensions;
	protected String _skippedDirectories;
	protected String _longestPath;
	protected String _longestFilename;
	protected String _largestFilename;

	protected boolean _deepScan = true;
	protected boolean _sortByCount = true;
	protected boolean _checkSkipList = false;

	protected int _totalFileCount = 0;
	protected int _totalDirectoryCount = 0;
	protected int _componentCount = 0;
	protected int _longestPathLength = 0;
	protected int _longestFilenameLength = 0;

	protected long _totalSize = 0L;
	protected long _largestFileSize = 0L;

	protected Map<String, FileExtensionCount> _extensionCount = new HashMap<String, FileExtensionCount>();
	protected Map<String, String> _components = new HashMap<String, String>();
	protected Map<String, Boolean> _componentExtensions = new HashMap<String, Boolean>();
	protected Map<String, Boolean> _skipExtensions = new HashMap<String, Boolean>();
	protected Map<String, Boolean> _skipDirectories = new HashMap<String, Boolean>();

	public Directory(SessionManager session, Element action) {
		super(session, action, false);

		_path = removeFinalSlash(_session.getAttribute(_action, "Path"));
		_destinationPath = removeFinalSlash(_session.getAttribute(_action, "DestinationPath"));
		_newName = _session.getAttribute(_action, "NewName");

		if (StringUtilities.isNullOrEmpty(_path)) {
			throw new RuntimeException(String.format("%s is missing a value for Path.", _actionName));
		}
		_session.addLogMessage("", "Path", _path);
	}

	@Override
	public String executeAction() {
		try {
			switch (_actionType) {
			case "Copy":
				return copyDirectory();
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
				throw new IOException(String.format("%s is not currently supported.", _actionType));
			}
		} catch (IOException ex) {
			throw new RuntimeException(String.format("%s could not %s %s.", _actionName, _actionType.toLowerCase(), _path), ex);
		}
	}

	protected String removeFinalSlash(String path) {
		if (StringUtilities.isNotNullOrEmpty(path) && (path.endsWith(File.separator))) {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}

	protected String deleteDirectory() throws IOException {
		int iLevels = _path.length() - _path.replace(File.separator, "").length();
		if (iLevels == 0) {
			throw new RuntimeException(String.format("%s requires at least one directory level (%s).", _actionName, _path));
		}
		File fi = new File(_path);
		if (!fi.exists()) {
			_session.addLogMessage("", "Process", String.format("%s does not exist, nothing to delete.", _path));
		} else if (fi.isFile()) {
			throw new IOException(String.format("%s is a file, use File.Delete to remove.", _path));
		} else {
			_session.addLogMessage("", "Process", String.format("Deleting %s", _path));
			FileUtils.deleteDirectory(new File(_path));
			_session.addLogMessage("", "", "Completed");
		}
		return "";
	}

	protected String createDirectory() throws IOException {
		File fi = new File(_path);
		if (!fi.exists()) {
			_session.addLogMessage("", "Process", String.format("Creating %s", _path));
			new File(_path).mkdirs();
			_session.addLogMessage("", "", "Completed");
		} else if (fi.isDirectory()) {
			_session.addLogMessage("", "Process", "Directory already exists, nothing to do.");
		} else if (fi.isFile()) {
			throw new IOException(String.format("%s is the name of an existing file.", _path));
		}
		return "";
	}

	protected String renameDirectory() throws IOException {
		File fi = new File(_path);
		if (!fi.exists()) {
			throw new RuntimeException(String.format("Directory %s not found, nothing to rename.", _path));
		} else if (fi.isDirectory()) {
			_session.addLogMessage("", "Process", String.format("Renaming %s to %s.", _path, _newName));
			fi.renameTo(new File(_newName));
			_session.addLogMessage("", "", "Completed");
		} else if (fi.isFile()) {
			throw new RuntimeException(String.format("%s is a file.  Use the File.Rename operations to work with files.", _path));
		}
		return "";
	}

	protected String moveDirectory() throws IOException {
		if (StringUtilities.isNullOrEmpty(_destinationPath)) {
			throw new RuntimeException(String.format("%s is missing a value for DestinationPath.", _actionName));
		}
		if (FileUtilities.isValidDirectory(_destinationPath)) {
			throw new RuntimeException(String.format("Destination directory (%s) already exists.", _destinationPath));
		}
		_session.addLogMessage("", "Destination Path", _destinationPath);
		_session.addLogMessage("", "Process", "Moving directory");
		FileUtils.moveDirectoryToDirectory(new File(_path), new File(_destinationPath), true);
		return "";
	}
	
	protected String copyDirectory() throws IOException {
		if (StringUtilities.isNullOrEmpty(_destinationPath)) {
			throw new RuntimeException(String.format("%s is missing a value for DestinationPath.", _actionName));
		}
		if (FileUtilities.isValidDirectory(_destinationPath)) {
			throw new RuntimeException(String.format("Destination directory (%s) already exists.", _destinationPath));
		}
		_session.addLogMessage("", "Destination Path", _destinationPath);
		_session.addLogMessage("", "Process", "Copy directory");
		FileUtils.copyDirectory(new File(_path), new File(_destinationPath), true);
		return "";
	}

	protected String profileDirectory() {
		_componentExtensions.put("jar", true);
		_componentExtensions.put("dll", true);

		String sDeep = _session.getAttribute(_action, "Deep");
		if (StringUtilities.isNotNullOrEmpty(sDeep)) {
			_session.addLogMessage("", "Deep", sDeep);
		}
		_deepScan = StringUtilities.toBoolean(sDeep, true);

		_skippedExtensions = _session.getAttribute(_action, "SkipExtensions");
		if (StringUtilities.isNotNullOrEmpty(_skippedExtensions)) {
			_session.addLogMessage("", "Skip Extensions", _skippedExtensions);
			String[] aSkipExtensions = _skippedExtensions.toLowerCase().split(",");
			if (aSkipExtensions.length > 0) {
				for (int i = 0; i < aSkipExtensions.length; i++) {
					_skipExtensions.put(aSkipExtensions[i], true);
				}
				_checkSkipList = true;
			}
		}

		_skippedDirectories = _session.getAttribute(_action, "SkipDirectories");
		if (StringUtilities.isNotNullOrEmpty(_skippedDirectories)) {
			_session.addLogMessage("", "Skip Directories", _skippedDirectories);
			String[] aSkipDirectories = _skippedDirectories.toLowerCase().split(",");
			if (aSkipDirectories.length > 0) {
				for (int i = 0; i < aSkipDirectories.length; i++) {
					_skipDirectories.put(aSkipDirectories[i], true);
				}
				_checkSkipList = true;
			}
		}

		String sortByCount = _session.getAttribute(_action, "SortByCount");
		if (StringUtilities.isNotNullOrEmpty(sortByCount)) {
			_session.addLogMessage("", "Sort by Count", sortByCount);
			_sortByCount = StringUtilities.toBoolean(sortByCount, true);
		}

		scanDirectory(_path);

		Object[] aCounts = _extensionCount.values().toArray();
		Arrays.sort(aCounts);

		// Write the report
		ReportBuilder sbReport = new ReportBuilder();
		sbReport.appendFormatLine("Date Scanned: %1$s", DateUtilities.getCurrentDateTime());
		sbReport.appendFormatLine("Path: %1$s", _path);
		if (_checkSkipList && StringUtilities.isNotNullOrEmpty(_skippedExtensions)) {
			sbReport.appendFormatLine("Skipped File Extensions: %1$s", _skippedExtensions);
		}
		if (_checkSkipList && StringUtilities.isNotNullOrEmpty(_skippedDirectories)) {
			sbReport.appendFormatLine("Skipped Directories: %1$s", _skippedDirectories);
		}
		sbReport.appendLine("");
		sbReport.appendFormatLine("Total File Count: %,d", _totalFileCount);
		sbReport.appendFormatLine("Total Directory Count: %,d", _totalDirectoryCount);
		sbReport.appendFormatLine("Component Files: %,d", _componentCount);
		sbReport.appendLine("");
		sbReport.appendFormatLine("Longest Path (%,d chars): %s", _longestPathLength, _longestPath);
		sbReport.appendFormatLine("Longest Filename (%,d chars): %s", _longestFilenameLength, _longestFilename);
		sbReport.appendLine("");
		sbReport.appendFormatLine("Largest File: (%,d bytes): %s", _largestFileSize, _largestFilename);
		sbReport.appendFormatLine("Total Space: %,d bytes", _totalSize);
		sbReport.appendLine("");
		sbReport.appendLine("File Extensions (sorted by count): ");

		int iLen = aCounts.length;
		for (int i = 0; i < iLen; i++) {
			FileExtensionCount fileExtensionCount = (FileExtensionCount) aCounts[i];
			sbReport.appendFormatLine("   *.%1$s: %2$,d", fileExtensionCount.getExtension(), fileExtensionCount.getCount());
		}
		_session.addLogMessage("", "Profile Results", "View Report", "file://" + FileUtilities.writeRandomTextFile(_session.getLogPath(), sbReport.toString()));
		return "";
	}

	protected void scanDirectory(String path) {
		File[] aContents = new File(path).listFiles();
		if ((aContents == null) || (aContents.length == 0))
			return;

		if (path.length() > _longestPathLength) {
			_longestPathLength = path.length();
			_longestPath = path;
		}

		for (File currentFile : aContents) {
			if (currentFile.isDirectory() && _deepScan) {
				String subPath = currentFile.getAbsolutePath();
				subPath = subPath.substring(subPath.lastIndexOf(File.separatorChar)).toLowerCase();
				if (_checkSkipList && _skipDirectories.containsKey(subPath)) {
					continue;
				}
				_totalDirectoryCount++;
				scanDirectory(currentFile.getAbsolutePath());
				continue;
			}

			String sExtension = FilenameUtils.getExtension(currentFile.getName()).toLowerCase();
			if (_checkSkipList && _skipExtensions.containsKey(sExtension)) {
				continue;
			}

			_totalFileCount++;
			_totalSize += currentFile.length();

			if (currentFile.getName().length() > _longestFilenameLength) {
				_longestFilenameLength = currentFile.getName().length();
				_longestFilename = currentFile.getName();
			}

			if (currentFile.length() > _largestFileSize) {
				_largestFileSize = currentFile.length();
				_largestFilename = currentFile.getName();
			}

			if (_componentExtensions.containsKey(sExtension)) {
				_componentCount++;
			}

			FileExtensionCount cnt = _extensionCount.get(sExtension);
			if (cnt == null) {
				_extensionCount.put(sExtension, new FileExtensionCount(sExtension, _sortByCount));
			} else {
				cnt.increment();
			}
		}
	}

	protected class FileExtensionCount implements Comparable<FileExtensionCount> {
		protected boolean _SortByCount = true;
		protected String _Extension;
		protected int _Count = 1;

		public FileExtensionCount(String extension) {
			this(extension, true);
		}

		public FileExtensionCount(String extension, boolean sortByCount) {
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
