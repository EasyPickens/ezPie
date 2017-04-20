/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPIE/wiki/Fannie-Mae-Open-Source-Licensing-Project
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.devtools.pie.actions;

import java.io.File;

import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.FileUtilities;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-05-24
 * 
 */

public class Maven extends RunCommand {
	protected String _batchFilename;

	public Maven(SessionManager session, Element action) {
		super(session, action, false);

		_workDirectory = requiredAttribute(action, "LocalPath").trim();
		if (FileUtilities.isInvalidDirectory(_workDirectory)) {
			throw new RuntimeException(String.format("LocalPath %s does not exist.", _workDirectory));
		}

		String pomFile = _workDirectory + "POM.xml";
		if (!_workDirectory.endsWith(File.separator)) {
			pomFile = _workDirectory + File.separator + "POM.xml";
		}
		if (FileUtilities.isInvalidFile(pomFile)) {
			throw new RuntimeException(String.format("No POM.xml file found in %s", pomFile));
		}

		StringBuilder sb = new StringBuilder();
		sb.append("mvn dependency:copy-dependencies");

		_session.addLogMessage("", "Maven Command", sb.toString());
		_batchFilename = FileUtilities.writeRandomFile(_session.getStagingPath(), "bat", sb.toString());
		_session.addLogMessage("", "Batch File", _batchFilename);
		_arguments = new String[] { _batchFilename };
	}
}
