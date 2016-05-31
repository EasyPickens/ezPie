package com.fanniemae.devtools.pie.actions;

import java.io.File;

import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.SessionManager;

//<Rename 
//   Source 
//   Destination 
///>

public class Rename extends Copy {

	public Rename(SessionManager session, Element action) {
		super(session, action);
	}
	
	@Override
	public String execute() {
		processFileSystem(_source, _destination);
		return null;
	}

	@Override
	protected void processFileSystem(String source, String destination) {
		try {
			File originalName = new File(source);
			File newName = new File(destination);
			if (newName.getParent() == null) {
				String fullPath = String.format("%s%s%s", originalName.getParent(), File.separator, newName.getName());
				newName = new File(fullPath);
			}
			originalName.renameTo(newName);
			_session.addLogMessage("", "Rename Complete", String.format("%s to %s", originalName, newName));
		} catch (Exception e) {
			throw new RuntimeException("Could not rename %s to %s.  " + e.getMessage(), e);
		}
	}
}
