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

package com.fanniemae.ezpie.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.common.XmlUtilities;
import com.fanniemae.ezpie.data.connectors.SqlConnector;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @author Tara Tritt
 * @since 2016-05-31
 * 
 */

public abstract class FileSystemAction extends Action {

	protected String _source;
	protected String _destination;
	protected String _includeFilter = null;
	protected String _excludeFilter = null;

	protected String _countMessage = "";

	protected boolean _shallow = false;
	protected boolean _skipHidden = false;
	protected boolean _clearReadOnly = false;
	protected boolean _required = true;
	protected boolean _hasIncludeFilter = false;
	protected boolean _hasExcludeFilter = false;

	protected Pattern[] _excludeRegex = null;
	protected Pattern[] _includeRegex = null;

	protected long _totalBytes = 0L;
	protected int _filesFound = 0;
	protected int _filesProcessed = 0;

	protected StringBuilder _sb = new StringBuilder();

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
			List<Pattern> exclude = new ArrayList<Pattern>();
			List<Pattern> include = new ArrayList<Pattern>();
			for (int i = 0; i < length; i++) {
				String name = nlChildren.item(i).getNodeName();
				Element child = (Element) nlChildren.item(i);
				switch (name) {
				case "Exclude":
					_hasExcludeFilter = true;
					exclude.add(Pattern.compile(_session.getAttribute(child, "Regex")));
					break;
				case "Include":
					_hasIncludeFilter = true;
					include.add(Pattern.compile(_session.getAttribute(child, "Regex")));
					break;
				case "ExcludeDBResultSet":
					_hasExcludeFilter = true;
					List<Pattern> excludes = executeCommand(child);
					exclude.addAll(excludes);
					break;
				case "ExcludeDirectory":
				case "IncludeDirectory":
				case "ExcludeFile":
				case "IncludeFile":
					throw new RuntimeException(String.format("%s child element no longer supported. Use Include or Exclude.", name));
				default:
					_session.addLogMessage("** Warning **", name, "Operation not currently supported.");
				}
			}
			_excludeRegex = new Pattern[exclude.size()];
			_excludeRegex = exclude.toArray(_excludeRegex);
			_includeRegex = new Pattern[include.size()];
			_includeRegex = include.toArray(_includeRegex);
			if (_excludeRegex.length > 0 && _includeRegex.length > 0) {
				throw new RuntimeException("Cannot have both Exclude and Include child elements. Create a seperate element.");
			}
		}
	}

	@Override
	public String executeAction(HashMap<String, String> dataTokens) {
		_session.setDataTokens(dataTokens);
		processFileSystem(_source, _destination);
		if (FileUtilities.isValidDirectory(_source)) {
			postprocessDirectory(_source);
		}
		// _session.addLogMessage("", String.format("%s Complete", _actionName), String.format("%,d files (%,d bytes)", _filesProcessed, _totalBytes));
		if (_actionName.equals("VerifyJavaFiles")) {
			_sb.append("Verification completed.");
			String removedFiles = (_filesProcessed > 0) ? "file://"+FileUtilities.writeRandomFile(_session.getLogPath(), ".txt", _sb.toString()) : "";
			_session.addLogMessage("", "Count", String.format("%,d empty JAVA files found, %,d files checked.", _filesProcessed, _filesFound), removedFiles);
		} else if (_actionName.equals("DeleteEmpty")) {
			_session.addLogMessage("", "Count", String.format("%,d empty files %s, %,d files checked.", _filesProcessed, _countMessage, _filesFound));
		} else {
			_session.addLogMessage("", "Count", String.format("%,d files (%,d bytes) %s, %,d files checked.", _filesProcessed, _totalBytes, _countMessage, _filesFound));
		}
		_session.clearDataTokens();
		return null;
	}

	protected List<Pattern> executeCommand(Element element) {
		String command = _session.getAttribute(element, "Command");
		if (StringUtilities.isNullOrEmpty(command))
			throw new RuntimeException(String.format("Missing a value for Command on the %s element.", element.getNodeName()));

		String name = _session.getAttribute(element, "Name");
		if (StringUtilities.isNullOrEmpty(name))
			element.setAttribute("Name", "ExcludeDBResultSet");

		SqlConnector conn = new SqlConnector(_session, element, false);
		conn.open();

		List<Pattern> excludes = new ArrayList<Pattern>();

		String[][] schema = conn.getDataSourceSchema();
		String col = _session.getAttribute(element, "Column");
		int colIndex = 0;
		for (int i = 0; i < schema.length; i++) {
			if (col.equals(schema[i][0])) {
				colIndex = i;
			}
		}

		while (!conn.eof()) {
			Object[] aValues = conn.getDataRow();
			String value = escapeSpecialChar(aValues[colIndex].toString());
			excludes.add(Pattern.compile(value));
		}

		conn.close();

		return excludes;

	}

	protected abstract void processFile(String source, String destination, String nameOnly);

	protected void postprocessDirectory(String source) {
	}

	protected void processFileSystem(String source, String destination) {
		processFileSystem(source, destination, !_hasIncludeFilter);
	}

	protected void processFileSystem(String source, String destination, boolean included) {
		File sourceLocation = new File(source);
		if (!sourceLocation.exists()) {
			if (_required) {
				throw new RuntimeException(String.format("%s does not exist.  To make this action optional, set the attribute Required to False.", source));
			}
			_session.addLogMessage("", "** Warning **", String.format(" Nothing found to %s. %s does not exist.", _actionName, source));
			return;
		} else if (sourceLocation.isFile()) {
			_totalBytes += FileUtilities.getLength(source);
			//_filesFound++;

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
			if (contents[i].isFile())
				_filesFound++;

			String entryName = contents[i].getName();
			String entryPath = contents[i].getAbsolutePath();
			if (_hasIncludeFilter && (matchesRegexFilter(entryName, _includeRegex) || matchesRegexFilter(entryPath, _includeRegex))) {
				// include filters override exclude filters
			} else if (_hasIncludeFilter && !matchesRegexFilter(entryName, _includeRegex) && !matchesRegexFilter(entryPath, _includeRegex) && !included) {
				if (contents[i].isDirectory()) {
					processFileSystem(contents[i].getPath(), destination + File.separator + entryName, false);
				}
				continue;
			} else if (_hasExcludeFilter && (matchesRegexFilter(entryName, _excludeRegex) || matchesRegexFilter(entryPath, _excludeRegex))) {
				continue;
			}

			if (contents[i].isDirectory()) {
				processFileSystem(contents[i].getPath(), destination + File.separator + entryName, true);
				postprocessDirectory(contents[i].getPath());
				continue;
			}

			_totalBytes += contents[i].length();
			//_filesFound++;
			processFile(contents[i].getPath(), destination, entryName);
		}
	}

	protected String escapeSpecialChar(String str) {
		String sep = "\\\\";
		return str.replaceAll("/", Matcher.quoteReplacement(sep));
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
