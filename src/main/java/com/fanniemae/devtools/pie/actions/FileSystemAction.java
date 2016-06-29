package com.fanniemae.devtools.pie.actions;

import java.io.File;
import java.io.FileFilter;

import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.FileUtilities;
import com.fanniemae.devtools.pie.common.StringUtilities;

public abstract class FileSystemAction extends Action {

	protected String _source;
	protected String _destination;
	protected String _includeFileFilter = null;
	protected String _excludeFileFilter = null;
	protected String _includeDirectoryFilter = null;
	protected String _excludeDirectoryFilter = null;
	protected String _type;

	protected boolean _shallow = false;
	protected boolean _skipHidden = false;
	protected boolean _clearReadOnly = false;
	protected boolean _isFile = false;
	protected boolean _isDirectory = false;
	protected boolean _hasIncludeFileFilter = false;
	protected boolean _hasExcludeFileFilter = false;
	protected boolean _hasIncludeDirectoryFilter = false;
	protected boolean _hasExcludeDirectoryFilter = false;

	protected FileFilter _includeFiles;
	protected FileFilter _excludeFiles;
	protected FileFilter _includeDirectories;
	protected FileFilter _excludeDirectories;

	protected long _totalBytes = 0L;
	protected int _filesProcessed = 0;

	public FileSystemAction(SessionManager session, Element action) {
		super(session, action, false);

		_includeFileFilter = optionalAttribute("IncludeFiles", null);
		if (StringUtilities.isNotNullOrEmpty(_includeFileFilter)) {
			_hasIncludeFileFilter = true;
			String[] filter = StringUtilities.split(_includeFileFilter);
			_includeFiles = new WildcardFileFilter(filter, IOCase.INSENSITIVE);
			_session.addLogMessage("", "IncludeFiles", _includeFileFilter);
		}

		_includeDirectoryFilter = optionalAttribute("IncludeDirectories", null);
		if (StringUtilities.isNotNullOrEmpty(_includeDirectoryFilter)) {
			_hasIncludeDirectoryFilter = true;
			String[] filter = StringUtilities.split(_includeDirectoryFilter);
			_includeDirectories = new WildcardFileFilter(filter, IOCase.INSENSITIVE);
			_session.addLogMessage("", "IncludeDirectories", _includeDirectoryFilter);
		}

		_excludeFileFilter = optionalAttribute("ExcludeFiles", null);
		if (StringUtilities.isNotNullOrEmpty(_excludeFileFilter)) {
			_hasExcludeFileFilter = true;
			String[] filter = StringUtilities.split(_excludeFileFilter);
			_excludeFiles = new WildcardFileFilter(filter, IOCase.INSENSITIVE);
			_session.addLogMessage("", "ExcludeFiles", _excludeFileFilter);
		}

		_excludeDirectoryFilter = optionalAttribute("ExcludeDirectories", null);
		if (StringUtilities.isNotNullOrEmpty(_excludeDirectoryFilter)) {
			_hasExcludeDirectoryFilter = true;
			String[] filter = StringUtilities.split(_excludeDirectoryFilter);
			_excludeDirectories = new WildcardFileFilter(filter, IOCase.INSENSITIVE);
			_session.addLogMessage("", "ExcludeDirectories", _excludeDirectoryFilter);
		}

		String shallow = optionalAttribute("Shallow", null);
		if (StringUtilities.isNotNullOrEmpty(shallow)) {
			_shallow = StringUtilities.toBoolean(shallow, false);
			_session.addLogMessage("", "Shallow", _shallow ? "True" : "False");
		}

		String skipHidden = optionalAttribute("SkipHidden", null);
		if (StringUtilities.isNotNullOrEmpty(skipHidden)) {
			_skipHidden = StringUtilities.toBoolean(skipHidden, false);
			_session.addLogMessage("", "SkipHidden", _skipHidden ? "True" : "False");
		}

		String clearReadOnly = optionalAttribute("ClearReadOnly", null);
		if (StringUtilities.isNotNullOrEmpty(clearReadOnly)) {
			_clearReadOnly = StringUtilities.toBoolean(clearReadOnly, false);
			_session.addLogMessage("", "ClearReadOnly", _clearReadOnly ? "True" : "False");
		}
	}

	@Override
	public String execute() {
		processFileSystem(_source, _destination);
		postprocessDirectory(_source);
		_session.addLogMessage("", String.format("%s Complete", _actionName), String.format("%,d files (%,d bytes)", _filesProcessed, _totalBytes));
		return null;
	}

	protected abstract void processFile(String source, String destination, String nameOnly);

	protected void postprocessDirectory(String source) {
	}

	protected void processFileSystem(String source, String destination) {
		if (_isFile) {
			_totalBytes += FileUtilities.getLength(source);
			_filesProcessed++;

			if (_actionName.equals("Delete") || (destination == null)) {
				processFile(source, null, null);
			} else if (FileUtilities.isValidDirectory(destination)) {
				// Destination is a directory, not file name - so use original file name
				File sourceFile = new File(source);
				processFile(source, destination, sourceFile.getName());
			} else {
				// Destination is assumed to be a full filename.
				File destinationFile = new File(destination);
				processFile(source, destinationFile.getParent(), destinationFile.getName());
			}
			return;
		}
		File dir = new File(source);
		File[] allEntries = dir.listFiles();
		File[] includeFiles = dir.listFiles(_includeFiles);
		File[] excludeFiles = (_hasExcludeFileFilter) ? dir.listFiles(_excludeFiles) : null;
		File[] includeDirectories = dir.listFiles(_includeDirectories);
		File[] excludeDirectories = (_hasExcludeDirectoryFilter) ? dir.listFiles(_excludeDirectories) : null;

		if (allEntries == null)
			return;

		for (int i = 0; i < allEntries.length; i++) {
			if (_skipHidden && allEntries[i].isHidden())
				continue;

			Boolean copyFile = true;
			String entryName = allEntries[i].getName();

			if (allEntries[i].isDirectory()) {
				if (_hasExcludeDirectoryFilter && inArray(entryName, excludeDirectories)) {
					continue;
				} else if (_hasIncludeDirectoryFilter && !inArray(entryName, includeDirectories)) {
					continue;
				}
				processFileSystem(allEntries[i].getPath(), destination + File.separator + entryName);
				postprocessDirectory(allEntries[i].getPath());
				continue;
			}

			// Exclude filters trump include
			if (_hasExcludeFileFilter && inArray(entryName, excludeFiles)) {
				copyFile = false;
			}

			// If it is still set to be copied, check against include filter
			if (copyFile && _hasIncludeFileFilter) {
				copyFile = inArray(entryName, includeFiles);
			}

			if (copyFile) {
				_totalBytes += allEntries[i].length();
				_filesProcessed++;
				processFile(allEntries[i].getPath(), destination, entryName);
			}
		}
	}

	protected boolean inArray(String value, File[] list) {
		if (list == null)
			return false;

		boolean found = false;
		for (int x = 0; x < list.length; x++) {
			if (list[x].getName().equals(value)) {
				found = true;
				break;
			}
		}
		return found;
	}

}
