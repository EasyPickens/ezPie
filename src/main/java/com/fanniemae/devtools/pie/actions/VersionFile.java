package com.fanniemae.devtools.pie.actions;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.FileUtilities;
import com.fanniemae.devtools.pie.common.StringUtilities;

public class VersionFile extends Action {
	protected String _path;
	protected String _filename = "Version.txt";
	protected String _version;
	
	protected Boolean _setVersion = false;

	public VersionFile(SessionManager session, Element action) {
		super(session, action, true);

		_path = requiredAttribute("Path");
		_filename = FileUtilities.combine(_path, _filename);
		
		_version = _session.getAttribute(action, "Version");
		if (StringUtilities.isNotNullOrEmpty(_version)) {
			_setVersion = true;
			_session.addLogMessage("", "Set Version", _version);
		} else {
			_session.addLogMessage("", "Version", "Read version from file.");
		}
	}

	@Override
	public String execute() {
		Properties prop = new Properties();
		if (_setVersion) {
			Date dt = new Date();
			prop.setProperty("Description", "Version information file generated by FNMA PIE component.");
			prop.setProperty("Date_Created", dt.toString());
			prop.setProperty("Version", _version);
			try (FileOutputStream output = new FileOutputStream(_filename)) {
				prop.store(output, null);
				output.close();
			} catch (IOException ex) {
				throw new RuntimeException(String.format("Error while writing %s the version file. %s",_filename,ex.getMessage()));
			}
		} else {
			if (FileUtilities.isInvalidFile(_filename)) {
				throw new RuntimeException(String.format("File %s was not found.", _filename));
			}
			try (FileInputStream input = new FileInputStream(_filename)) {
				prop.load(input);
			} catch (IOException ex) {
				throw new RuntimeException(String.format("Error while trying to read %s the version file. %s", _filename, ex.getMessage()));
			}
			_session.addToken("Local", _id, prop.getProperty("Version", ""));
		}
		return null;
	}

}
