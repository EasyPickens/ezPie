package com.fanniemae.devtools.pie.actions;

import java.io.File;

import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.SessionManager;

public class Rename extends Copy {

	public Rename(SessionManager session, Element action) {
		super(session, action);
		_countMessage = "renamed";
	}

	@Override
	public String executeAction() {
		processFileSystem(_source, _destination);
		return null;
	}

	@Override
	protected void processFileSystem(String source, String destination) {
		File originalName = new File(source);
		if (originalName.exists()) {
			File newName = new File(destination);
			if (newName.getParent() == null) {
				String fullPath = String.format("%s%s%s", originalName.getParent(), File.separator, newName.getName());
				newName = new File(fullPath);
			}
			originalName.renameTo(newName);
			_session.addLogMessage("", "Rename Complete", String.format("%s to %s", originalName, newName));
		} else {
			if (_required) {
				throw new RuntimeException(String.format("%s does not exist.  To make this action optional, set the attribute Required to False.", source));
			}
			_session.addLogMessage("", "** Warning **", String.format(" Nothing found to %s. %s does not exist.", _actionName, source));
		}
	}
}
