package com.fanniemae.devtools.pie.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.FileUtilities;
import com.fanniemae.devtools.pie.common.StringUtilities;
import com.fanniemae.devtools.pie.common.XmlUtilities;

public abstract class FileSystemAction extends Action {

	protected String _source;
	protected String _destination;
	protected String _includeFileFilter = null;
	protected String _excludeFileFilter = null;
	protected String _includeDirectoryFilter = null;
	protected String _excludeDirectoryFilter = null;

	protected boolean _shallow = false;
	protected boolean _skipHidden = false;
	protected boolean _clearReadOnly = false;
	protected boolean _required = true;
	protected boolean _hasIncludeFileFilter = false;
	protected boolean _hasExcludeFileFilter = false;
	protected boolean _hasIncludeDirectoryFilter = false;
	protected boolean _hasExcludeDirectoryFilter = false;

	protected Pattern[] _excludeDirectoryRegex = null;
	protected Pattern[] _includeDirectoryRegex = null;
	protected Pattern[] _excludeFileRegex = null;
	protected Pattern[] _includeFileRegex = null;

	protected long _totalBytes = 0L;
	protected int _filesProcessed = 0;

	public FileSystemAction(SessionManager session, Element action) {
		super(session, action, false);

		String required = optionalAttribute("Required", null);
		if (StringUtilities.isNotNullOrEmpty(required)) {
			_required = StringUtilities.toBoolean(required, true);
		}
		
		String shallow = optionalAttribute("Shallow", null);
		if (StringUtilities.isNotNullOrEmpty(shallow)) {
			_shallow = StringUtilities.toBoolean(shallow, false);
		}

		String skipHidden = optionalAttribute("SkipHidden", null);
		if (StringUtilities.isNotNullOrEmpty(skipHidden)) {
			_skipHidden = StringUtilities.toBoolean(skipHidden, false);
		}

		String clearReadOnly = optionalAttribute("ClearReadOnly", null);
		if (StringUtilities.isNotNullOrEmpty(clearReadOnly)) {
			_clearReadOnly = StringUtilities.toBoolean(clearReadOnly, false);
		}

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
					_hasExcludeDirectoryFilter = true;
					excludeDirectory.add(Pattern.compile(_session.getAttribute(child, "Regex")));
					break;
				case "IncludeDirectory":
					_hasIncludeDirectoryFilter = true;
					includeDirectory.add(Pattern.compile(_session.getAttribute(child, "Regex")));
					break;
				case "ExcludeFile":
					_hasExcludeFileFilter = true;
					excludeFile.add(Pattern.compile(_session.getAttribute(child, "Regex")));
					break;
				case "IncludeFile":
					_hasIncludeFileFilter = true;
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
		processFileSystem(_source, _destination);
		if (FileUtilities.isValidDirectory(_source)) {
			postprocessDirectory(_source);
		}
		_session.addLogMessage("", String.format("%s Complete", _actionName), String.format("%,d files (%,d bytes)", _filesProcessed, _totalBytes));
		return null;
	}

	protected abstract void processFile(String source, String destination, String nameOnly);

	protected void postprocessDirectory(String source) {
	}

	protected void processFileSystem(String source, String destination) {
		File sourceLocation = new File(source);
		if (!sourceLocation.exists()) {
			if (_required) {
				throw new RuntimeException(String.format("%s does not exist.  To make this action optional, set the attribute Required to False.", source));
			}
			_session.addLogMessage("", "** Warning **", String.format(" Nothing found to %s. %s does not exist.", _actionName, source));
			return;
		} else if (sourceLocation.isFile()) {
			_totalBytes += FileUtilities.getLength(source);
			_filesProcessed++;

			if (_actionName.equals("Delete")) {
				processFile(source, null, null);
			} else if (destination == null) {
				// Destination is null and file action is not delete?
				throw new RuntimeException("Destination path is null and file action is not delete?");
			} else if (FileUtilities.isValidDirectory(destination)) {
				// Destination is an existing directory, not file name - so use original file name
				File sourceFile = new File(source);
				processFile(source, destination, sourceFile.getName());
			} else {
				// Destination is assumed to be a full filename.
				File destinationFile = new File(destination);
				processFile(source, destinationFile.getParent(), destinationFile.getName());
			}
			return;
		}

		File[] contents = sourceLocation.listFiles();
		if ((contents == null) || (contents.length == 0))
			return;

		for (int i = 0; i < contents.length; i++) {
			if (_skipHidden && contents[i].isHidden())
				continue;

			String entryName = contents[i].getName();
			if (contents[i].isDirectory()) {
				if (_hasIncludeDirectoryFilter && matchesRegexFilter(entryName, _includeDirectoryRegex)) {
					// include filters override exclude filters
				} else if (_hasExcludeDirectoryFilter && matchesRegexFilter(entryName, _excludeDirectoryRegex)) {
					continue;
				}

				processFileSystem(contents[i].getPath(), destination + File.separator + entryName);
				postprocessDirectory(contents[i].getPath());
				continue;
			}

			if (_hasIncludeFileFilter && matchesRegexFilter(entryName, _includeFileRegex)) {
				// include filters override exclude filters
			} else if (_hasExcludeFileFilter && matchesRegexFilter(entryName, _excludeFileRegex)) {
				continue;
			}
			_totalBytes += contents[i].length();
			_filesProcessed++;
			processFile(contents[i].getPath(), destination, entryName);
		}
	}

	protected boolean inArray(String value, File[] list) {
		if (list == null)
			return false;

		boolean found = false;
		for (int x = 0; x < list.length; x++) {
			if (list[x].getName().equalsIgnoreCase(value)) {
				found = true;
				break;
			}
		}
		return found;
	}

	protected boolean matchesRegexFilter(String value, Pattern[] regexFilter) {
		if (regexFilter == null)
			return false;

		for (int x = 0; x < regexFilter.length; x++) {
			Matcher m = regexFilter[x].matcher(value);
			if (m.find()) {
				return true;
			}
		}
		return false;
	}

}
