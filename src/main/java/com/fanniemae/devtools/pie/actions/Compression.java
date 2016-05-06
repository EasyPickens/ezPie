package com.fanniemae.devtools.pie.actions;

import java.io.IOException;

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
	protected String _includeFilter = null;
	protected String _excludeFilter = null;

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
		
		String includeFilter = _session.getAttribute(action, "IncludeFilter");
		String excludeFilter = _session.getAttribute(action, "ExcludeFilter");
		_includeFilter = StringUtilities.isNullOrEmpty(includeFilter) ? null : includeFilter;
		_excludeFilter = StringUtilities.isNullOrEmpty(excludeFilter) ? null : excludeFilter;
		_deep = _session.getAttribute(action, "Deep").toLowerCase().equals("false") ? false : true;
		
		if (StringUtilities.isNotNullOrEmpty(_includeFilter)) {
			_session.addLogMessage("", "Include Filter", _includeFilter);
		}
		
		if (StringUtilities.isNotNullOrEmpty(_excludeFilter)) {
			_session.addLogMessage("", "Exclude Filter", _excludeFilter);
		}
	}

	@Override
	public String execute() {
		try {
			if (_zip) {
				String filelist = ArrayUtilities.toString(ZipUtilities.zip(_sourcePath, _zipFilename));
				_session.addLogMessageHtml("", "Files Compressed", filelist);
				_session.addLogMessage("", "Zipped Size", String.format("%,d bytes", FileUtilities.getLength(_zipFilename)));
				_session.addToken("File", _id, _zipFilename);
			} else {
				String[] list = ZipUtilities.unzip(_zipFilename, _destinationPath);
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
