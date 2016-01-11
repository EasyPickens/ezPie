package com.fanniemae.automation.data.connectors;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import com.fanniemae.automation.SessionManager;
import com.fanniemae.automation.common.FileUtilities;
import com.fanniemae.automation.common.StringUtilities;

public class FileSystemConnector extends DataConnector {

	protected String _Path;
	protected String _SkippedExtensions;
	protected String _SkippedDirectories;

	protected Boolean _DeepScan = true;
	protected Boolean _CheckSkipList = false;

	protected File _FileInfo;
	protected File[] _Contents;

	protected Map<String, Boolean> _SkipExtensions = new HashMap<String, Boolean>();
	protected Map<String, Boolean> _SkipDirectories = new HashMap<String, Boolean>();

	public FileSystemConnector(SessionManager session, Element dataSource, Boolean isSchemaOnly) {
		super(session, dataSource, isSchemaOnly);

		_Path = _Session.getAttribute(dataSource, "Path");
		if (StringUtilities.isNullOrEmpty(_Path)) {
			throw new RuntimeException("FileSystemConnector requires a Path to a directory.");
		} else if (FileUtilities.isInvalidDirectory(_Path)) {
			throw new RuntimeException(String.format("Directory (%s) not found.", _Path));
		}

		String sDeep = _Session.getAttribute(_DataSource, "Deep");
		if (StringUtilities.isNotNullOrEmpty(sDeep)) {
			_Session.addLogMessage("", "Deep", sDeep);
		}
		_DeepScan = StringUtilities.toBoolean(sDeep, true);

		_SkippedExtensions = _Session.getAttribute(_DataSource, "SkipExtensions");
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

		_SkippedDirectories = _Session.getAttribute(_DataSource, "SkipDirectories");
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

		_IncludedDirectories = _Session.getAttribute(_DataSource, "IncludeDirectories");
		if (StringUtilities.isNotNullOrEmpty(_SkippedDirectories)) {
			_Session.addLogMessage("", "Include Directories", _IncludedDirectories);
			String[] aSkipDirectories = _IncludedDirectories.toLowerCase().split(",");
			if (aSkipDirectories.length > 0) {
				for (int i = 0; i < aSkipDirectories.length; i++) {
					_IncludeDirectories.put(aSkipDirectories[i], true);
				}
				_CheckIncludeList = true;
			}
		}

		_Session.addLogMessagePreserveLayout("", "Path", _Path);

		_FileInfo = new File(_Path);
		_Contents = _FileInfo.listFiles();
	}

	@Override
	public Boolean open() {
		// Populate file array for this directory.
		return true;
	}

	@Override
	public Boolean eof() {
		// as long as index is less than current array and we have a directory
		// to check, EOF is false.
		return null;
	}

	@Override
	public Object[] getDataRow() {
		// return a row populated with basic file information. Name, Path,
		// FullPath, Extension, Date, Size,
		return null;
	}

	@Override
	public void close() {
		// Nothing to close?
	}

	protected Map<String, Boolean> parseCsv(String value, String logMessage) {
		Map<String, Boolean> aResult = new HashMap<String, Boolean>();

		if (StringUtilities.isNotNullOrEmpty(value)) {
			_Session.addLogMessage("", logMessage, value);
			String[] aSkipExtensions = value.toLowerCase().split(",");
			if (aSkipExtensions.length > 0) {
				for (int i = 0; i < aSkipExtensions.length; i++) {
					aResult.put(aSkipExtensions[i], true);
				}
			}
		}
		return aResult;
	}

}
