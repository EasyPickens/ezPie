package com.fanniemae.automation.data.connectors;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Element;

import com.fanniemae.automation.SessionManager;
import com.fanniemae.automation.common.DataStream;
import com.fanniemae.automation.common.FileUtilities;
import com.fanniemae.automation.common.StringUtilities;
import com.fanniemae.automation.datafiles.DataReader;
import com.fanniemae.automation.datafiles.DataWriter;

public class DirectoryConnector extends DataConnector {

	protected String _Path;
	protected String _SkippedExtensions;
	protected String _SkippedDirectories;
	protected String _IncludedExtensions;
	protected String _IncludedDirectories;

	protected Boolean _DeepScan = true;
	protected Boolean _CheckSkipList = false;
	protected Boolean _CheckIncludeList = false;
	protected Boolean _ListFiles = true;
	protected Boolean _ListDirectories = false;

	protected Object[] _DataRow = new Object[8];
	protected static String[] _ColumnNames = new String[] { "Name", "Type", "Size", "Modified", "FullPath", "BaseName", "Extension", "PathOnly" };
	protected static String[] _DataTypes = new String[] { "StringData", "StringData", "LongData", "DateData", "StringData", "StringData", "StringData", "StringData" };

	protected DataStream _DataStream;
	protected DataWriter _dw;
	protected DataReader _dr;

	protected Map<String, Boolean> _SkipExtensions = new HashMap<String, Boolean>();
	protected Map<String, Boolean> _SkipDirectories = new HashMap<String, Boolean>();
	protected Map<String, Boolean> _IncludeExtensions = new HashMap<String, Boolean>();
	protected Map<String, Boolean> _IncludeDirectories = new HashMap<String, Boolean>();

	public DirectoryConnector(SessionManager session, Element dataSource, Boolean isSchemaOnly) {
		super(session, dataSource, isSchemaOnly);

		_Path = _Session.getAttribute(dataSource, "Path");
		if (StringUtilities.isNullOrEmpty(_Path)) {
			throw new RuntimeException("DataSource.Directory requires a valid Path.");
		} else if (FileUtilities.isInvalidDirectory(_Path)) {
			throw new RuntimeException(String.format("Directory (%s) not found.", _Path));
		}
		_Session.addLogMessagePreserveLayout("", "Path", _Path);

		String sDeep = _Session.getAttribute(_DataSource, "Deep");
		if (StringUtilities.isNotNullOrEmpty(sDeep)) {
			_Session.addLogMessage("", "Deep", sDeep);
			_DeepScan = StringUtilities.toBoolean(sDeep, true);
		}
		
		String sListFiles = _Session.getAttribute(_DataSource, "ListFiles");
		if (StringUtilities.isNotNullOrEmpty(sListFiles)) {
			_Session.addLogMessage("", "List Files", sListFiles);
			_ListFiles = StringUtilities.toBoolean(sListFiles, true);
		}
		
		String sListDirectories = _Session.getAttribute(_DataSource, "ListDirectories");
		if (StringUtilities.isNotNullOrEmpty(sListDirectories)) {
			_Session.addLogMessage("", "List Directories", sListDirectories);
			_ListDirectories = StringUtilities.toBoolean(sListDirectories, false);
		}

		_SkippedExtensions = _Session.getAttribute(_DataSource, "SkipExtensions");
		_SkipExtensions = toHashMap(_SkippedExtensions, "Skip Extensions", true);

		_SkippedDirectories = _Session.getAttribute(_DataSource, "SkipDirectories");
		_SkipDirectories = toHashMap(_SkippedDirectories, "Skip Directories", true);

		_IncludedExtensions = _Session.getAttribute(_DataSource, "IncludeExtensions");
		_IncludeExtensions = toHashMap(_IncludedExtensions, "Skip Extensions", true);

		_IncludedDirectories = _Session.getAttribute(_DataSource, "IncludeDirectories");
		_IncludeDirectories = toHashMap(_IncludedDirectories, "Include Directories", false);

		_DataSchema = new String[_ColumnNames.length][2];
		for (int i = 0; i < _ColumnNames.length; i++) {
			_DataSchema[i][0] = _ColumnNames[i];
			_DataSchema[i][1] = _DataTypes[i];
		}

		saveDirectory(_Path);
	}

	@Override
	public Boolean open() {
		try {
			_dr = new DataReader(_DataStream);
		} catch (IOException ex) {
			throw new RuntimeException("Could not open data stream.", ex);
		}
		return true;
	}

	@Override
	public Boolean eof() {
		try {
			return _dr.eof();
		} catch (IOException ex) {
			throw new RuntimeException("Could not read EOF indicator.", ex);
		}
	}

	@Override
	public Object[] getDataRow() {
		try {
			return _dr.getRowValues();
		} catch (IOException ex) {
			throw new RuntimeException("Error reading data row values. " + ex.getMessage(), ex);
		}
	}

	@Override
	public void close() {
		try {
			_dr.close();
		} catch (Exception ex) {
			throw new RuntimeException("Tried to close data stream.", ex);
		}
	}

	protected Map<String, Boolean> toHashMap(String value, String logMessage, Boolean skipList) {
		Map<String, Boolean> aResult = new HashMap<String, Boolean>();

		if (StringUtilities.isNotNullOrEmpty(value)) {
			_Session.addLogMessage("", logMessage, value);
			String[] aSkipExtensions = value.toLowerCase().split(",");
			if (aSkipExtensions.length > 0) {
				for (int i = 0; i < aSkipExtensions.length; i++) {
					aResult.put(aSkipExtensions[i], true);
					if (skipList) {
						_CheckSkipList = true;
					} else {
						_CheckIncludeList = true;
					}
				}
			}
		}
		return aResult;
	}

	protected void saveDirectory(String path) {
		String sTempFilename = FileUtilities.getRandomFilename(_Session.getStagingPath());
		try (DataWriter dw = new DataWriter(sTempFilename, 10);) {
			_dw = dw;
			dw.SetupDataColumns(_DataSchema);
			scanDirectory(_Path);
			dw.close();
			_DataStream = dw.getDataStream();
		} catch (Exception ex) {
			throw new RuntimeException(String.format("Error while trying to scan directory %s.", _Path), ex);
		}
	}

	protected void scanDirectory(String path) throws IOException {
		File[] aContents = new File(path).listFiles();
		if ((aContents == null) || (aContents.length == 0))
			return;

		for (File currentEntry : aContents) {

			if (currentEntry.isDirectory()) {
				String subFolder = currentEntry.getAbsolutePath();
				subFolder = subFolder.substring(subFolder.lastIndexOf(File.separatorChar)).toLowerCase();
				if (_CheckSkipList && _SkipDirectories.containsKey(subFolder)) {
					continue;
				} else if (_CheckIncludeList && !_IncludeDirectories.containsKey(subFolder)) {
					continue;
				}
				if (_ListDirectories) {
					saveRowValues(currentEntry);
				}
				if (_DeepScan) {
					scanDirectory(currentEntry.getAbsolutePath());
				}
				continue;
			}

			String sExtension = FilenameUtils.getExtension(currentEntry.getName()).toLowerCase();
			if (_CheckSkipList && _SkipExtensions.containsKey(sExtension)) {
				continue;
			} else if (_CheckIncludeList && !_IncludeExtensions.containsKey(sExtension)) {
				continue;
			} else if (_ListFiles) {
				saveRowValues(currentEntry);
			}
		}
	}

	protected void saveRowValues(File entry) throws IOException {
		_DataRow[0] = entry.getName();
		_DataRow[1] = entry.isDirectory() ? "Directory" : "File";
		_DataRow[2] = entry.isDirectory() ? 0L : entry.length();
		_DataRow[3] = new Date(entry.lastModified());
		_DataRow[4] = entry.getAbsolutePath();
		_DataRow[5] = FilenameUtils.getBaseName(entry.getName());
		_DataRow[6] = FilenameUtils.getExtension(entry.getName());
		_DataRow[7] = entry.getParent();
		_dw.WriteDataRow(_DataRow);
	}

}
