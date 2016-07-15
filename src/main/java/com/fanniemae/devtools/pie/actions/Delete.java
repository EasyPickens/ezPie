package com.fanniemae.devtools.pie.actions;

import java.io.File;

import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.FileUtilities;

public class Delete extends FileSystemAction {

	protected boolean _exists = true;

	public Delete(SessionManager session, Element action) {
		super(session, action);
		_source = requiredAttribute("Path", String.format("%s action requires a Path to a directory or file.", _actionName));
	}

	@Override
	protected void processFile(String source, String destination, String nameOnly) {
		try {
			File sourceFile = new File(source);
			if (!sourceFile.exists()) {
				return;
			} else if (_clearReadOnly && !sourceFile.canWrite()) {
				sourceFile.setWritable(true);
			}
			sourceFile.delete();
		} catch (Exception e) {
			RuntimeException ex = new RuntimeException(String.format("Error while trying to delete %s. Message is %s", source, e.getMessage()), e);
			throw ex;
		}
	}

	@Override
	protected void postprocessDirectory(String source) {
		// remove the empty directories
		if (FileUtilities.isEmptyDirectory(source)) {
			try {
				File dir = new File(source);
				dir.delete();
			} catch (Exception e) {
				RuntimeException ex = new RuntimeException(String.format("Could not remove empty directory (%s) during move operation. %s", source, e.getMessage()), e);
				throw ex;
			}
		}
	}
}
