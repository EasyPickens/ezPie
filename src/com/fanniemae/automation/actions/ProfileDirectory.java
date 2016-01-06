package com.fanniemae.automation.actions;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
 * @since 2016-01-04
 * 
 */
public class ProfileDirectory extends Action {

	protected String _Path;
	protected String _LongestPath;
	protected String _LongestFilename;
	protected String _LargestFilename;

	protected String[] _ExcludeExtensions;
	protected String[] _ExcludeDirectories;

	protected Boolean _Deep = true;

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

	public ProfileDirectory(SessionManager session, Element eleAction) {
		super(session, eleAction, false);

		_ComponentExtensions.put("jar", true);
		_ComponentExtensions.put("dll", true);
		_Path = _Session.getAttribute(_Action, "Path");
		if (StringUtilities.isNullOrEmpty(_Path)) {
			throw new RuntimeException("ProfileDirectory requires a value for Path.");
		} else if (FileUtilities.isInvalidDirectory(_Path)) {
			throw new RuntimeException(String.format("Can not profile directory %s, because it does not exist.", _Path));
		}
		_Session.addLogMessage("", "Path", _Path);

		String sDeep = _Session.getAttribute(_Action, "Deep");
		if (StringUtilities.isNotNullOrEmpty(sDeep)) {
			_Session.addLogMessage("", "Deep", sDeep);
		}
		_Deep = StringUtilities.toBoolean(sDeep, true);

		String sExclude = _Session.getAttribute(_Action, "ExcludeExtensions");
		if (StringUtilities.isNotNullOrEmpty(sExclude)) {
			_Session.addLogMessage("", "Exclude Extensions", sExclude);
			_ExcludeExtensions = sExclude.toLowerCase().split(",");
		}

		String sExcludeDirectories = _Session.getAttribute(_Action, "ExcludeDirectories");
		if (StringUtilities.isNotNullOrEmpty(sExcludeDirectories)) {
			_Session.addLogMessage("", "Exclude Directories", sExcludeDirectories);
			_ExcludeDirectories = sExclude.toLowerCase().split(",");
		}
	}

	@Override
	public String execute() {

		scanDirectory(_Path);

		Object[] aCounts = _ExtensionCount.values().toArray();
		Arrays.sort(aCounts);

		// Write the report
		ReportBuilder sbReport = new ReportBuilder();
		sbReport.appendFormatLine("Date Scanned: %1$s", DateUtilities.getCurrentDateTime());
		sbReport.appendFormatLine("Path: %1$s", _Path);
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
		sbReport.appendLine("File Count By Extension: ");

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

		for (File fi : aContents) {
			if (fi.isDirectory()) {
				_TotalDirectoryCount++;
				scanDirectory(fi.getAbsolutePath());
			}

			_TotalFileCount++;

			if (fi.getName().length() > _LongestFilenameLength) {
				_LongestFilenameLength = fi.getName().length();
				_LongestFilename = fi.getName();
			}

			_TotalSize += fi.length();

			if (fi.length() > _LargestFileSize) {
				_LargestFileSize = fi.length();
				_LargestFilename = fi.getName();
			}

			String sExtension = FilenameUtils.getExtension(fi.getName()).toLowerCase();
			if (_ComponentExtensions.containsKey(sExtension)) {
				_ComponentCount++;
			}
			FileExtensionCount cnt = _ExtensionCount.get(sExtension);
			if (cnt == null) {
				_ExtensionCount.put(sExtension, new FileExtensionCount(sExtension));
			} else {
				cnt.increment();
			}
		}
	}

	protected class FileExtensionCount implements Comparable<FileExtensionCount> {

		protected String _Extension;
		protected int _Count = 1;

		public FileExtensionCount(String extension) {
			if (extension == null)
				extension = "";
			_Extension = extension;
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
			return o._Count - this._Count;
			//return this._Extension.compareTo(o._Extension);
		}
	}

}
