package com.fanniemae.devtools.pie.actions;

import java.io.File;

import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.FileUtilities;
import com.fanniemae.devtools.pie.common.StringUtilities;

public class Maven extends RunCommand {
	protected String _batchFilename;

	public Maven(SessionManager session, Element action) {
		super(session, action, false);

		_workDirectory = _session.getAttribute(action, "LocalPath").trim();
		if (StringUtilities.isNullOrEmpty(_workDirectory)) {
			throw new RuntimeException("No LocalPath specified for Maven action.");
		} else if (FileUtilities.isInvalidDirectory(_workDirectory)) {
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
