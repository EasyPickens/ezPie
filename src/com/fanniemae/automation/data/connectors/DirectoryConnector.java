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

/**
 * 
 * @author Richard Monson
 * @since 2016-01-12
 * 
 */
public class DirectoryConnector extends DataConnector {

	protected String _Path;
	protected String _SkippedExtensions;
	protected String _SkippedFolders;
	protected String _IncludedExtensions;
	protected String _IncludedFolders;

	protected Boolean _doFullScan = true;
	protected Boolean _hasSkipExtensions = false;
	protected Boolean _hasSkipFolders = false;
	protected Boolean _hasIncludeExtensions = false;
	protected Boolean _hasIncludeFolders = false;
	protected Boolean _outputFiles = true;
	protected Boolean _outputFolders = false;

	protected Object[] _DataRow = new Object[8];
	protected static String[] _ColumnNames = new String[] { "Name", "Type", "Size", "Modified", "FullPath", "BaseName", "Extension", "PathOnly" };
	protected static String[] _DataTypes = new String[] { "StringData", "StringData", "LongData", "DateData", "StringData", "StringData", "StringData", "StringData" };

	protected DataStream _DataStream;
	protected DataWriter _dw;
	protected DataReader _dr;

	protected Map<String, Boolean> _SkipExtensions = new HashMap<String, Boolean>();
	protected Map<String, Boolean> _SkipFolders = new HashMap<String, Boolean>();
	protected Map<String, Boolean> _IncludeExtensions = new HashMap<String, Boolean>();
	protected Map<String, Boolean> _IncludeFolders = new HashMap<String, Boolean>();

	public DirectoryConnector(SessionManager session, Element dataSource, Boolean isSchemaOnly) {
		super(session, dataSource, isSchemaOnly);

		_Path = _Session.getAttribute(dataSource, "Path");
		if (StringUtilities.isNullOrEmpty(_Path)) {
			throw new RuntimeException("DataSource.Directory requires a valid Path.");
		} else if (FileUtilities.isInvalidDirectory(_Path)) {
			throw new RuntimeException(String.format("Directory (%s) not found.", _Path));
		}
		_Session.addLogMessagePreserveLayout("", "Path", _Path);

		String sFullScan = _Session.getAttribute(_DataSource, "FullScan");
		if (StringUtilities.isNotNullOrEmpty(sFullScan)) {
			_Session.addLogMessage("", "Full Scan", sFullScan);
			_doFullScan = StringUtilities.toBoolean(sFullScan, true);
		}
		
		String sListFiles = _Session.getAttribute(_DataSource, "ListFiles");
		if (StringUtilities.isNotNullOrEmpty(sListFiles)) {
			_Session.addLogMessage("", "List Files", sListFiles);
			_outputFiles = StringUtilities.toBoolean(sListFiles, true);
		}
		
		String sListFolders = _Session.getAttribute(_DataSource, "ListFolders");
		if (StringUtilities.isNotNullOrEmpty(sListFolders)) {
			_Session.addLogMessage("", "List Directories", sListFolders);
			_outputFolders = StringUtilities.toBoolean(sListFolders, false);
		}

		_SkippedExtensions = _Session.getAttribute(_DataSource, "SkipExtensions");
		_SkipExtensions = toHashMap(_SkippedExtensions, "Skip Extensions", true);
		_hasSkipExtensions = (_SkipExtensions.size() > 0) ? true : false;

		_SkippedFolders = _Session.getAttribute(_DataSource, "SkipFolders");
		_SkipFolders = toHashMap(_SkippedFolders, "Skip Directories", true);
		_hasSkipFolders = (_SkipFolders.size() > 0) ? true : false;

		_IncludedExtensions = _Session.getAttribute(_DataSource, "IncludeExtensions");
		_IncludeExtensions = toHashMap(_IncludedExtensions, "Include Extensions", false);
		_hasIncludeExtensions = (_IncludeExtensions.size() > 0) ? true : false;

		_IncludedFolders = _Session.getAttribute(_DataSource, "IncludeFolders");
		_IncludeFolders = toHashMap(_IncludedFolders, "Include Directories", false);
		_hasIncludeExtensions = (_IncludeExtensions.size() > 0) ? true : false;

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
			return _dr.getDataRow();
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
				}
			}
		}
		return aResult;
	}

	protected void saveDirectory(String path) {
		String sTempFilename = FileUtilities.getRandomFilename(_Session.getStagingPath());
		try (DataWriter dw = new DataWriter(sTempFilename, 10);) {
			_dw = dw;
			dw.setDataColumns(_DataSchema);
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
				subFolder = subFolder.substring(subFolder.lastIndexOf(File.separatorChar)+1).toLowerCase();
				if (_hasSkipFolders && _SkipFolders.containsKey(subFolder)) {
					continue;
				} else if (_hasIncludeFolders && !_IncludeFolders.containsKey(subFolder)) {
					continue;
				}
				if (_outputFolders) {
					saveRowValues(currentEntry);
				}
				if (_doFullScan) {
					scanDirectory(currentEntry.getAbsolutePath());
				}
				continue;
			}

			String sExtension = FilenameUtils.getExtension(currentEntry.getName()).toLowerCase();
			if (_hasSkipExtensions && _SkipExtensions.containsKey(sExtension)) {
				continue;
			} else if (_hasIncludeExtensions && !_IncludeExtensions.containsKey(sExtension)) {
				continue;
			} else if (_outputFiles) {
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
		_dw.writeDataRow(_DataRow);
	}

}
