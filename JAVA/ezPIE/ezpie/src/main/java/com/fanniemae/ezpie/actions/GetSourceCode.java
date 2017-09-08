/**
 *  
 * Copyright (c) 2017 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
**/

package com.fanniemae.ezpie.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.ArrayUtilities;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.ReportBuilder;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.common.XmlUtilities;
import com.fanniemae.ezpie.datafiles.DataReader;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2017-04-19
 * 
 **/
public class GetSourceCode extends RunCommand {

	protected List<String> _usedDirectories = new ArrayList<String>();
	
	protected String _timeoutString = "2h"; 

	public GetSourceCode(SessionManager session, Element action) {
		super(session, action, false);
	}

	@Override
	public String executeAction(HashMap<String, String> dataTokens) {
		_session.setDataTokens(dataTokens);
		String datasetName = requiredAttribute("DataSetName").trim();
		String locationTypeColumn = requiredAttribute("LocationTypeColumn").trim();
		String locationColumn = requiredAttribute("LocationColumn").trim();
		String directoryColumn = optionalAttribute("DirectoryColumn", "directory_name");
		_timeoutString = optionalAttribute("Timeout","2h");

		_workDirectory = requiredAttribute("LocalPath").trim();
		if (FileUtilities.isInvalidDirectory(_workDirectory)) {
			File file = new File(_workDirectory);
			file.mkdirs();
		}

		_session.addToken("LocalData", "aip_appname", "MyTestApplication");
		// build command file
		ReportBuilder batchCommands = new ReportBuilder();
		try (DataReader dr = new DataReader(_session.getDataStream(datasetName))) {
			String[] columnNames = dr.getColumnNames();

			int locationTypeIndex = ArrayUtilities.indexOf(columnNames, locationTypeColumn);
			int locationIndex = ArrayUtilities.indexOf(columnNames, locationColumn);
			int directoryIndex = ArrayUtilities.indexOf(columnNames, directoryColumn);

			if (locationTypeIndex == -1) {
				throw new RuntimeException(String.format("DataSetName %s does not contain a column named %s", datasetName, locationTypeColumn));
			}
			if (locationIndex == -1) {
				throw new RuntimeException(String.format("DataSetName %s does not contain a column named %s", datasetName, locationColumn));
			}

			_usedDirectories = new ArrayList<String>();
			while (!dr.eof()) {
				Object[] dataRow = dr.getDataRow();

				String codeLocation = (String) dataRow[locationIndex];
				if (StringUtilities.isNullOrEmpty(codeLocation)) {
					continue;
				}
				int codeLocationType = convertLocationType(codeLocation, dataRow[locationTypeIndex]);
				String directoryName = uniqueDirectoryName(codeLocationType, codeLocation, (directoryIndex > -1) ? (String) dataRow[locationIndex] : null);
				String localPath = FileUtilities.addDirectory(_workDirectory, directoryName);

				switch (codeLocationType) {
				case 1:
					// SVN
					batchCommands.appendFormatLine("svn checkout %s %s", codeLocation, StringUtilities.wrapValue(localPath));
					break;
				case 2:
					// Git
					Document gitDocument = XmlUtilities.createXMLDocument(String.format("<script><GitClone URL=\"%s\" LocalPath=\"%s\" Timeout=\"%s\" /></script>", codeLocation, localPath, _timeoutString));
					Action git = new GitClone(_session, (Element) gitDocument.getDocumentElement().getFirstChild());
					git.execute(_dataTokens);
					break;
				case 3:
					// File path
					Document copyDocument = XmlUtilities.createXMLDocument(String.format("<script><Copy Source=\"%s\" Destination=\"%s\" /></script>", codeLocation, localPath));
					Action copyFiles = new Copy(_session, (Element) copyDocument.getDocumentElement().getFirstChild());
					copyFiles.execute(_dataTokens);
					break;
				case 4:
					// Zip file
					Document unzipDocument = XmlUtilities.createXMLDocument(String.format("<script><Unzip ZipFilename=\"%s\" Destination=\"%s\" /></script>", codeLocation, localPath));
					Action unzipFiles = new Compression(_session, (Element) unzipDocument.getDocumentElement().getFirstChild());
					unzipFiles.execute(_dataTokens);
					break;
				default:
					throw new RuntimeException(String.format("Requested code location type (%s) not currently supported.", codeLocationType));
				}
			}
			dr.close();
			if (batchCommands.hasText()) {
				_session.addLogMessage("RunCommand", "Process", String.format("Processing %s action (started: %s)", "RunCommand", _sdf.format(new Date())));
				_session.addLogMessage("", "Commands", batchCommands.toString());
				String batchFilename = FileUtilities.writeRandomFile(_session.getStagingPath(), "bat", batchCommands.toString());
				_session.addLogMessage("", "Batch File", batchFilename);
				_arguments = new String[] { batchFilename };
				super.executeAction(_dataTokens);
			}
		} catch (Exception e) {
			RuntimeException ex = new RuntimeException("Error retrieving the source code.", e);
			throw ex;
		}

		_session.clearDataTokens();
		return null;
	}

	protected String uniqueDirectoryName(int codeLocationType, String codeLocation, String directoryName) {
		if (StringUtilities.isNullOrEmpty(directoryName) || (_usedDirectories.contains(directoryName.toLowerCase()))) {
			if (StringUtilities.isNullOrEmpty(directoryName) && StringUtilities.isNotNullOrEmpty(codeLocation)) {
				// Use the last level of the URL or the zip filename as a directory name.
				if (codeLocationType <= 2) {
					directoryName = lastUrlSegment(codeLocation);
				} else if (codeLocationType <= 4) {
					directoryName = lastFileSystemSegment(codeLocation);
				}
			}
			if (StringUtilities.isNullOrEmpty(directoryName)) {
				directoryName = "directory";
			}
			// Verify directories are unique, if not add a number to the name
			if (_usedDirectories.contains(directoryName.toLowerCase())) {
				for (int i = 0; i < 500; i++) {
					String tempDirectoryName = String.format("%s_%d", directoryName.toLowerCase(), i);
					if (!_usedDirectories.contains(tempDirectoryName)) {
						directoryName = String.format("%s_%d", directoryName, i);
						break;
					}
				}
			}
			_usedDirectories.add(directoryName.toLowerCase());
		}
		return directoryName;
	}

	protected String lastUrlSegment(String url) {
		if (url == null)
			return null;

		String lastDirectory;
		if (url.endsWith("/")) {
			lastDirectory = url.substring(0, url.length() - 1);
		} else if (url.toLowerCase().endsWith(".git")) {
			lastDirectory = url.substring(0, url.length() - 4);
		} else {
			lastDirectory = url;
		}
		return lastDirectory.substring(lastDirectory.lastIndexOf('/') + 1);
	}

	protected String lastFileSystemSegment(String zipFilename) {
		if (zipFilename == null)
			return null;
		String lastDirectory = zipFilename.toLowerCase().endsWith(".zip") ? zipFilename.substring(0, zipFilename.length() - 4) : zipFilename;
		int lastForwardSlash = lastDirectory.lastIndexOf('/');
		int lastBackSlash = lastDirectory.lastIndexOf('\\');
		return lastDirectory.substring(Math.max(lastForwardSlash, lastBackSlash) + 1);
	}

	protected int convertLocationType(String codeUrl, Object codeLocationType) {
		if (codeLocationType == null) {
			throw new RuntimeException(String.format("Missing code location type for %s", codeUrl));
		} else if (codeLocationType.getClass().getName().indexOf("String") > -1) {
			String locationType = (String) codeLocationType;
			switch (locationType.toLowerCase()) {
			case "svn":
				return 1;
			case "git":
				return 2;
			case "file":
				return 3;
			case "zip":
				return 4;
			default:
				throw new RuntimeException(String.format("Requested code location type (%s) not currently supported.", locationType));
			}
		}
		return (int) codeLocationType;
	}
}
