package com.fanniemae.devtools.pie.actions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.ArrayUtilities;
import com.fanniemae.devtools.pie.common.FileUtilities;
import com.fanniemae.devtools.pie.common.XmlUtilities;
import com.fanniemae.devtools.pie.common.ZipUtilities;

public class Compression extends Action {

	protected boolean _zip;
	protected boolean _deep = true;

	protected String _zipFilename;
	protected String _source;
	protected String _destination;

	protected Pattern[] _excludeDirectoryRegex = null;
	protected Pattern[] _includeDirectoryRegex = null;
	protected Pattern[] _excludeFileRegex = null;
	protected Pattern[] _includeFileRegex = null;

	public Compression(SessionManager session, Element action) {
		super(session, action, true);

		_zip = action.getNodeName().equals("Zip");

		_zipFilename = requiredAttribute("ZipFilename");
		if(!_zipFilename.endsWith(".zip")){
			_zipFilename += ".zip";
		} 
		if (!_zip && FileUtilities.isInvalidFile(_zipFilename)) {
			throw new RuntimeException(String.format("%s file not found.", _zipFilename));
		}

		if (_zip) {
			_source = requiredAttribute("Source");
			if (FileUtilities.isInvalidDirectory(_source)) {
				throw new RuntimeException("Zip error: Source does not exist.");
			}
		} else {
			_destination = requiredAttribute("Destination");
			if (FileUtilities.isInvalidDirectory(_destination)) {
				throw new RuntimeException("UnZip error: Destination does not exist.");
			}
		}
		
		_deep = optionalAttribute("Deep", "true").toLowerCase().equals("false") ? false : true;
		
		
		NodeList nlChildren = XmlUtilities.selectNodes(_action, "*");
		int length = nlChildren.getLength();
		if (length > 0) {
			List<Pattern> excludeDirectory = new ArrayList<Pattern>();
			List<Pattern> includeDirectory = new ArrayList<Pattern>();
			List<Pattern> excludeFile = new ArrayList<Pattern>();
			List<Pattern> includeFile = new ArrayList<Pattern>();
			for (int i = 0; i < length; i++) {
				String name = nlChildren.item(i).getNodeName();
				Element child = (Element) nlChildren.item(i);
				switch (name) {
				case "ExcludeDirectory":
					excludeDirectory.add(Pattern.compile(_session.getAttribute(child, "Regex")));
					break;
				case "IncludeDirectory":
					includeDirectory.add(Pattern.compile(_session.getAttribute(child, "Regex")));
					break;
				case "ExcludeFile":
					excludeFile.add(Pattern.compile(_session.getAttribute(child, "Regex")));
					break;
				case "IncludeFile":
					includeFile.add(Pattern.compile(_session.getAttribute(child, "Regex")));
					break;
				}
			}
			_excludeDirectoryRegex = new Pattern[excludeDirectory.size()];
			_excludeDirectoryRegex = excludeDirectory.toArray(_excludeDirectoryRegex);
			_includeDirectoryRegex = new Pattern[includeDirectory.size()];
			_includeDirectoryRegex = includeDirectory.toArray(_includeDirectoryRegex);
			_excludeFileRegex = new Pattern[excludeFile.size()];
			_excludeFileRegex = excludeFile.toArray(_excludeFileRegex);
			_includeFileRegex = new Pattern[includeFile.size()];
			_includeFileRegex = includeFile.toArray(_includeFileRegex);
		}
	}

	@Override
	public String executeAction() {
		try {
			if (_zip) {
				String filelist = ArrayUtilities.toString(ZipUtilities.zip(_source, _zipFilename, _includeFileRegex, _excludeFileRegex, _includeDirectoryRegex, _excludeDirectoryRegex));
				_session.addLogMessageHtml("", "Files Compressed", filelist);
				_session.addLogMessage("", "Zipped Size", String.format("%,d bytes", FileUtilities.getLength(_zipFilename)));
				_session.addToken("File", _id, _zipFilename);
			} else {
				String[] list = ZipUtilities.unzip(_zipFilename, _destination, _includeFileRegex, _excludeFileRegex);
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
