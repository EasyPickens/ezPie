package com.fanniemae.devtools.pie.actions;

import java.io.File;
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
		
		_zipFilename = _session.getAttribute(action, "ZipFilename");
		if (StringUtilities.isNullOrEmpty(_zipFilename)) {
			throw new RuntimeException("Missing required ZipFilename.");
		} else if (!_zip && FileUtilities.isInvalidFile(_zipFilename)) {
			throw new RuntimeException(String.format("%s file not found.", _zipFilename));
		}
		_session.addLogMessage("", "Zip Filename", _zipFilename);
		
		if (_zip) {
			String sourcePath = _session.getAttribute(action, "SourcePath");
			_sourcePath = StringUtilities.isNullOrEmpty(sourcePath) ? null : sourcePath;
			if (StringUtilities.isNullOrEmpty(_sourcePath)) {
				throw new RuntimeException("Zip error: Missing file SourcePath value.");
			} else if (FileUtilities.isInvalidDirectory(_sourcePath)) {
				throw new RuntimeException("Zip error: SourcePath does not exist.");
			}
			_session.addLogMessage("", "Source Path", _sourcePath);
		} else {
			String destinationPath = _session.getAttribute(action, "DestinationPath");
			_destinationPath = StringUtilities.isNullOrEmpty(destinationPath) ? null : destinationPath;
			if (StringUtilities.isNullOrEmpty(_destinationPath)) {
				throw new RuntimeException("UnZip error: Missing file DestinationPath value.");
			} else if (FileUtilities.isInvalidDirectory(_destinationPath)) {
				throw new RuntimeException("UnZip error: DestinationPath does not exist.");
			}
			_session.addLogMessage("", "Destination Path", _destinationPath);
		}
		
		_includeFileFilter = _session.getAttribute(action, "IncludeFiles");
		if (StringUtilities.isNotNullOrEmpty(_includeFileFilter)) {
			String[] filter = StringUtilities.split(_includeFileFilter);
			_includeFiles = new WildcardFileFilter(filter, IOCase.INSENSITIVE);
			_session.addLogMessage("", "IncludeFiles", _includeFileFilter);
		}

		_includeDirectoryFilter = _session.getAttribute(action, "IncludeDirectories");
		if (StringUtilities.isNotNullOrEmpty(_includeDirectoryFilter)) {
			String[] filter = StringUtilities.split(_includeDirectoryFilter);
			_includeDirectories = new WildcardFileFilter(filter, IOCase.INSENSITIVE);
			_session.addLogMessage("", "IncludeDirectories", _includeDirectoryFilter);
		}

		_excludeFileFilter = _session.getAttribute(action, "ExcludeFiles");
		if (StringUtilities.isNotNullOrEmpty(_excludeFileFilter)) {
			String[] filter = StringUtilities.split(_excludeFileFilter);
			_excludeFiles = new WildcardFileFilter(filter, IOCase.INSENSITIVE);
			_session.addLogMessage("", "ExcludeFiles", _excludeFileFilter);
		}

		_excludeDirectoryFilter = _session.getAttribute(action, "ExcludeDirectories");
		if (StringUtilities.isNotNullOrEmpty(_excludeDirectoryFilter)) {
			String[] filter = StringUtilities.split(_excludeDirectoryFilter);
			_excludeDirectories = new WildcardFileFilter(filter, IOCase.INSENSITIVE);
			_session.addLogMessage("", "ExcludeDirectories", _excludeDirectoryFilter);
		}

		_deep = _session.getAttribute(action, "Deep").toLowerCase().equals("false") ? false : true;
	
	}

	@Override
	public String execute() {
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
