package com.fanniemae.devtools.pie.actions;

import java.io.FileFilter;
import java.io.IOException;

import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.ArrayUtilities;
import com.fanniemae.devtools.pie.common.FileUtilities;
import com.fanniemae.devtools.pie.common.StringUtilities;
import com.fanniemae.devtools.pie.common.ZipUtilities;

public class Compression extends Action {

	protected boolean _zip;
	protected boolean _deep = true;

	protected String _zipFilename;
	protected String _sourcePath;
	protected String _destinationPath;

	protected String _includeFileFilter = null;
	protected String _excludeFileFilter = null;
	protected String _includeDirectoryFilter = null;
	protected String _excludeDirectoryFilter = null;

	protected FileFilter _includeFiles;
	protected FileFilter _excludeFiles;
	protected FileFilter _includeDirectories;
	protected FileFilter _excludeDirectories;

	public Compression(SessionManager session, Element action) {
		super(session, action, true);

		_zip = action.getNodeName().equals("Zip");

		_zipFilename = requiredAttribute("ZipFilename");
		if (!_zip && FileUtilities.isInvalidFile(_zipFilename)) {
			throw new RuntimeException(String.format("%s file not found.", _zipFilename));
		}
		_session.addLogMessage("", "Zip Filename", _zipFilename);

		if (_zip) {
			_sourcePath = requiredAttribute("SourcePath");
			if (FileUtilities.isInvalidDirectory(_sourcePath)) {
				throw new RuntimeException("Zip error: SourcePath does not exist.");
			}
			_session.addLogMessage("", "Source Path", _sourcePath);
		} else {
			_destinationPath = requiredAttribute("DestinationPath");
			if (FileUtilities.isInvalidDirectory(_destinationPath)) {
				throw new RuntimeException("UnZip error: DestinationPath does not exist.");
			}
			_session.addLogMessage("", "Destination Path", _destinationPath);
		}

		_includeFileFilter = optionalAttribute("IncludeFiles", null);
		if (StringUtilities.isNotNullOrEmpty(_includeFileFilter)) {
			String[] filter = StringUtilities.split(_includeFileFilter);
			_includeFiles = new WildcardFileFilter(filter, IOCase.INSENSITIVE);
			_session.addLogMessage("", "IncludeFiles", _includeFileFilter);
		}

		_includeDirectoryFilter = optionalAttribute("IncludeDirectories", null);
		if (StringUtilities.isNotNullOrEmpty(_includeDirectoryFilter)) {
			String[] filter = StringUtilities.split(_includeDirectoryFilter);
			_includeDirectories = new WildcardFileFilter(filter, IOCase.INSENSITIVE);
			_session.addLogMessage("", "IncludeDirectories", _includeDirectoryFilter);
		}

		_excludeFileFilter = optionalAttribute("ExcludeFiles", null);
		if (StringUtilities.isNotNullOrEmpty(_excludeFileFilter)) {
			String[] filter = StringUtilities.split(_excludeFileFilter);
			_excludeFiles = new WildcardFileFilter(filter, IOCase.INSENSITIVE);
			_session.addLogMessage("", "ExcludeFiles", _excludeFileFilter);
		}

		_excludeDirectoryFilter = optionalAttribute("ExcludeDirectories", null);
		if (StringUtilities.isNotNullOrEmpty(_excludeDirectoryFilter)) {
			String[] filter = StringUtilities.split(_excludeDirectoryFilter);
			_excludeDirectories = new WildcardFileFilter(filter, IOCase.INSENSITIVE);
			_session.addLogMessage("", "ExcludeDirectories", _excludeDirectoryFilter);
		}

		_deep = optionalAttribute("Deep", "true").toLowerCase().equals("false") ? false : true;

	}

	@Override
	public String executeAction() {
		try {
			if (_zip) {
				String filelist = ArrayUtilities.toString(ZipUtilities.zip(_sourcePath, _zipFilename, _includeFiles, _excludeFiles, _includeDirectories, _excludeDirectories));
				_session.addLogMessageHtml("", "Files Compressed", filelist);
				_session.addLogMessage("", "Zipped Size", String.format("%,d bytes", FileUtilities.getLength(_zipFilename)));
				_session.addToken("File", _id, _zipFilename);
			} else {
				String[] list = ZipUtilities.unzip(_zipFilename, _destinationPath, _includeFiles, _excludeFiles, _includeDirectories, _excludeDirectories);
				String filelist = ArrayUtilities.toString(list);
				_session.addLogMessageHtml("", "Files Decompressed", filelist);
				_session.addLogMessage("", "Count", String.format("%,d files", list.length - 2));
			}
		} catch (IOException ex) {
			_session.addErrorMessage(ex);
		}
		return null;
	}

}
