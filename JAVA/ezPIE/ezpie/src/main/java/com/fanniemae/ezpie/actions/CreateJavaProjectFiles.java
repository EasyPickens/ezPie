/**
 *  
 * Copyright (c) 2017 Fannie Mae, All rights reserved.
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

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.ReportBuilder;
import com.fanniemae.ezpie.common.StringUtilities;

/**
 *  
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2017-02-01
 *
 */

//@formatter:off
/**
 * Example Element:
 * <CreateJavaProjectFiles 
 *    Path="" 
 *    CodeDirectoryName - optional, defaults to "src" 
 *    OverwriteExisting - optional, defaults to false
 *    Shallow           - optional, defaults to false 
 * />
 * 
 */
//@formatter:on

public class CreateJavaProjectFiles extends Action {

	protected String _source;
	protected String _javaCodeDirectory = "src";
	protected Boolean _overwriteExisting = false;
	protected Boolean _shallow = false;
	protected Boolean _hasJavaCode = false;
	protected ArrayList<String> _usedAnalysisUnitNames = new ArrayList<String>();
	protected ReportBuilder _filecreationLog = new ReportBuilder();
	protected int _lineCount = 0;

	public CreateJavaProjectFiles(SessionManager session, Element action) {
		super(session, action, false);
		_source = requiredAttribute("Path", String.format("%s action requires a path value.", _actionName));
		_javaCodeDirectory = optionalAttribute("CodeDirectoryName", "src");
		_overwriteExisting = StringUtilities.toBoolean(optionalAttribute("OverwriteExisting", "False"), false);
		_shallow = StringUtilities.toBoolean(optionalAttribute("Shallow", "False"), false);
	}

	@Override
	public String executeAction(HashMap<String, String> dataTokens) {
		_session.setDataTokens(dataTokens);
		// Check to make sure path is available
		if (FileUtilities.isInvalidDirectory(_source)) {
			throw new RuntimeException(String.format("%s directory was not found.", _source));
		}
		processDirectory(_source);
		if (_filecreationLog.hasText()) {
			String creationLog = FileUtilities.writeRandomFile(_session.getLogPath(), "txt", _filecreationLog.toString());
			_session.addLogMessage("", "Files Created", String.format("View Creation Log (%,d lines)", _lineCount), "file://" + creationLog);
		}
		_session.clearDataTokens();
		return null;
	}

	protected void processDirectory(String path) {
		String analysisUnitName = "";
		// 1 Check for src directory
		File filePath = new File(path);
		if (!filePath.exists()) {
			throw new RuntimeException(String.format("%s does not exist.", path));
		} else if (!filePath.isDirectory()) {
			throw new RuntimeException(String.format("%s is not a directory.", path));
		}
		File[] contents = filePath.listFiles();
		boolean isCodeDirectory = false;
		for (int i = 0; i < contents.length; i++) {
			String name = contents[i].getName();
			if (contents[i].isFile()) {
				isCodeDirectory = (StringUtilities.isNotNullOrEmpty(name) && name.equalsIgnoreCase(".project")) ? true : isCodeDirectory;
			} else if (contents[i].isDirectory() && _javaCodeDirectory.equalsIgnoreCase(name)) {
				isCodeDirectory = true;
			} else if (contents[i].isDirectory() && !_shallow) {
				processDirectory(contents[i].getPath());
			}
		}
		if (isCodeDirectory) { //isCodeDirectory) { // && _hasJavaCode) {
			_hasJavaCode = false;
			String currentPath = path.endsWith(File.separator) ? path : path + File.separator;
			String projectFilename = currentPath + ".project";
			String classpathFilename = currentPath + ".classpath";

			if (FileUtilities.isInvalidFile(projectFilename) || (FileUtilities.isValidFile(projectFilename) && _overwriteExisting)) {
				_lineCount++;
				_filecreationLog.appendFormat("Creating project   file %s ...", projectFilename);
				analysisUnitName = createAnalysisUnitName(path, analysisUnitName);
				writeProjectFile(analysisUnitName, projectFilename);
				_usedAnalysisUnitNames.add(analysisUnitName);
				_filecreationLog.appendLine("Done");
			}

			if (FileUtilities.isInvalidFile(classpathFilename) || (FileUtilities.isValidFile(classpathFilename) && _overwriteExisting)) {
				_lineCount++;
				_filecreationLog.appendFormat("Creating classpath file %s ...", classpathFilename);
				writeClasspathFile(classpathFilename);
				_filecreationLog.appendLine("Done");
			}
		}
		return;
	}

	protected void writeProjectFile(String analysisUnitName, String projectFilename) {
		// Write the project file
		ReportBuilder rb = new ReportBuilder();
		rb.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?> ");
		rb.appendLine("<projectDescription> ");
		rb.appendFormatLine("   <name>%s</name> ", analysisUnitName);
		rb.appendLine("   <comment></comment> ");
		rb.appendLine("   <projects> ");
		rb.appendFormatLine("      <project>%s</project> ", analysisUnitName);
		rb.appendLine("   </projects> ");
		rb.appendLine("   <buildSpec> ");
		rb.appendLine("   </buildSpec> ");
		rb.appendLine("   <natures> ");
		rb.appendLine("      <nature>org.eclipse.jdt.core.javanature</nature> ");
		rb.appendLine("   </natures> ");
		rb.appendLine("</projectDescription>");
		FileUtilities.writeFile(projectFilename, rb.toString());
	}

	protected void writeClasspathFile(String classpathFilename) {
		// Write the classpath file
		ReportBuilder rb = new ReportBuilder();
		rb.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		rb.appendLine("<classpath> ");
		rb.appendFormatLine("   <classpathentry kind=\"%s\" output=\"target/classes\" path=\"java\"/>", _javaCodeDirectory);
		rb.appendLine("</classpath>");
		FileUtilities.writeFile(classpathFilename, rb.toString());
	}

	protected String createAnalysisUnitName(String currentPath, String currentAnalysisUnitName) {
		if (StringUtilities.isNotNullOrEmpty(currentAnalysisUnitName)) {
			return currentAnalysisUnitName;
		}

		String analysisUnitName = "";
		if (StringUtilities.isNullOrEmpty(currentPath) || StringUtilities.isNullOrEmpty(_source)) {
			throw new RuntimeException(String.format("Null path exception.  Current Path = %s, Source = %s", currentPath, _source));
		} else if (currentPath.length() == _source.length()) {
			analysisUnitName = currentPath.substring(currentPath.lastIndexOf(File.separatorChar) + 1);
		} else {
			analysisUnitName = currentPath.substring(_source.length() + 1).replace(File.separatorChar, '_');
		}
		if (analysisUnitName.length() > 60) {
			analysisUnitName = analysisUnitName.substring(0, 60);
		}
		if (StringUtilities.isNullOrEmpty(analysisUnitName) || _usedAnalysisUnitNames.contains(analysisUnitName.toLowerCase())) {
			boolean duplicateDetected = true;
			for (int i = 0; i < 500; i++) {
				String tempName = String.format("$s$d", analysisUnitName, i);
				if (!_usedAnalysisUnitNames.contains(tempName.toLowerCase())) {
					analysisUnitName = tempName;
					duplicateDetected = false;
					break;
				}
			}
			if (duplicateDetected) {
				throw new RuntimeException(String.format("Duplicate Analysis Unit name detected (%s)", analysisUnitName));
			}
		}
		return analysisUnitName;
	}

}
