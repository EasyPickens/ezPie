package com.fanniemae.devtools.pie.actions;

import java.io.File;

import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.StringUtilities;

/**
 * 
 * @author Richard Monson
 * @since 2016-06-03
 * 
 */

public class MakeDirectory extends Action {
	protected String _path;

	public MakeDirectory(SessionManager session, Element action) {
		super(session, action, false);
		_path = _session.getAttribute(action, "Path");
		if (StringUtilities.isNullOrEmpty(_path)) {
			throw new RuntimeException(String.format("%s action requires a value for the new Path.", _actionName));
		}
		_session.addLogMessage("", "Path", _path);
	}

	@Override
	public String execute() {
		File fi = new File(_path);
		if (!fi.exists()) {
			_session.addLogMessage("", "", String.format("Creating %s", _path));
			fi.mkdirs();
			_session.addLogMessage("", "", "Completed");
		} else if (fi.isDirectory()) {
			_session.addLogMessage("", "", "Directory already exists, nothing to do.");
		} else if (fi.isFile()) {
			throw new RuntimeException(String.format("%s is the name of an existing file.", _path));
		}
		return "";
	}

}
