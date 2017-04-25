/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.data.connectors;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.DataStream;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.datafiles.DataReader;
import com.fanniemae.ezpie.datafiles.DataWriter;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-01-12
 * 
 */

public class DirectoryConnector extends DataConnector {

	protected String _path;
	protected String _skippedExtensions;
	protected String _skippedFolders;
	protected String _includedExtensions;
	protected String _includedFolders;

	protected Boolean _doFullScan = true;
	protected Boolean _hasSkipExtensions = false;
	protected Boolean _hasSkipFolders = false;
	protected Boolean _hasIncludeExtensions = false;
	protected Boolean _hasIncludeFolders = false;
	protected Boolean _outputFiles = true;
	protected Boolean _outputFolders = false;

	protected Object[] _dataRow = new Object[8];
	protected static String[] COLUMN_NAMES = new String[] { "Name", "Type", "Size", "Modified", "FullPath", "BaseName", "Extension", "PathOnly" };
	protected static String[] DATA_TYPES = new String[] { "StringData", "StringData", "LongData", "DateData", "StringData", "StringData", "StringData", "StringData" };

	protected DataStream _dataStream;
	protected DataWriter _dw;
	protected DataReader _dr;

	protected Map<String, Boolean> _skipExtensions = new HashMap<String, Boolean>();
	protected Map<String, Boolean> _skipFolders = new HashMap<String, Boolean>();
	protected Map<String, Boolean> _includeExtensions = new HashMap<String, Boolean>();
	protected Map<String, Boolean> _includeFolders = new HashMap<String, Boolean>();

	public DirectoryConnector(SessionManager session, Element dataSource, Boolean isSchemaOnly) {
		super(session, dataSource, isSchemaOnly);

		_path = _session.getAttribute(dataSource, "Path");
		if (StringUtilities.isNullOrEmpty(_path)) {
			throw new RuntimeException("DataSource.Directory requires a valid Path.");
		} else if (FileUtilities.isInvalidDirectory(_path)) {
			throw new RuntimeException(String.format("Directory (%s) not found.", _path));
		}
		_session.addLogMessagePreserveLayout("", "Path", _path);

		String sFullScan = _session.getAttribute(_dataSource, "FullScan");
		if (StringUtilities.isNotNullOrEmpty(sFullScan)) {
			_session.addLogMessage("", "Full Scan", sFullScan);
			_doFullScan = StringUtilities.toBoolean(sFullScan, true);
		}
		
		String sListFiles = _session.getAttribute(_dataSource, "ListFiles");
		if (StringUtilities.isNotNullOrEmpty(sListFiles)) {
			_session.addLogMessage("", "List Files", sListFiles);
			_outputFiles = StringUtilities.toBoolean(sListFiles, true);
		}
		
		String sListFolders = _session.getAttribute(_dataSource, "ListFolders");
		if (StringUtilities.isNotNullOrEmpty(sListFolders)) {
			_session.addLogMessage("", "List Directories", sListFolders);
			_outputFolders = StringUtilities.toBoolean(sListFolders, false);
		}

		_skippedExtensions = _session.getAttribute(_dataSource, "SkipExtensions");
		_skipExtensions = toHashMap(_skippedExtensions, "Skip Extensions", true);
		_hasSkipExtensions = (_skipExtensions.size() > 0) ? true : false;

		_skippedFolders = _session.getAttribute(_dataSource, "SkipFolders");
		_skipFolders = toHashMap(_skippedFolders, "Skip Directories", true);
		_hasSkipFolders = (_skipFolders.size() > 0) ? true : false;

		_includedExtensions = _session.getAttribute(_dataSource, "IncludeExtensions");
		_includeExtensions = toHashMap(_includedExtensions, "Include Extensions", false);
		_hasIncludeExtensions = (_includeExtensions.size() > 0) ? true : false;

		_includedFolders = _session.getAttribute(_dataSource, "IncludeFolders");
		_includeFolders = toHashMap(_includedFolders, "Include Directories", false);
		_hasIncludeExtensions = (_includeExtensions.size() > 0) ? true : false;

		_dataSchema = new String[COLUMN_NAMES.length][2];
		for (int i = 0; i < COLUMN_NAMES.length; i++) {
			_dataSchema[i][0] = COLUMN_NAMES[i];
			_dataSchema[i][1] = DATA_TYPES[i];
		}

		saveDirectory(_path);
	}

	@Override
	public Boolean open() {
		try {
			_dr = new DataReader(_dataStream);
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
			_session.addLogMessage("", logMessage, value);
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
		String sTempFilename = FileUtilities.getRandomFilename(_session.getStagingPath());
		try (DataWriter dw = new DataWriter(sTempFilename, 10);) {
			_dw = dw;
			dw.setDataColumns(_dataSchema);
			scanDirectory(_path);
			dw.close();
			_dataStream = dw.getDataStream();
		} catch (Exception ex) {
			throw new RuntimeException(String.format("Error while trying to scan directory %s.", _path), ex);
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
				if (_hasSkipFolders && _skipFolders.containsKey(subFolder)) {
					continue;
				} else if (_hasIncludeFolders && !_includeFolders.containsKey(subFolder)) {
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
			if (_hasSkipExtensions && _skipExtensions.containsKey(sExtension)) {
				continue;
			} else if (_hasIncludeExtensions && !_includeExtensions.containsKey(sExtension)) {
				continue;
			} else if (_outputFiles) {
				saveRowValues(currentEntry);
			}
		}
	}

	protected void saveRowValues(File entry) throws IOException {
		_dataRow[0] = entry.getName();
		_dataRow[1] = entry.isDirectory() ? "Directory" : "File";
		_dataRow[2] = entry.isDirectory() ? 0L : entry.length();
		_dataRow[3] = new Date(entry.lastModified());
		_dataRow[4] = entry.getAbsolutePath();
		_dataRow[5] = FilenameUtils.getBaseName(entry.getName());
		_dataRow[6] = FilenameUtils.getExtension(entry.getName());
		_dataRow[7] = entry.getParent();
		_dw.writeDataRow(_dataRow);
	}

}
