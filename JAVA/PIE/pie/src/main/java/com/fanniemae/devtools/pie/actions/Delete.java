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
 * @since 2016-05-31
 * 
 */

public class Delete extends FileSystemAction {

	protected boolean _exists = true;

	public Delete(SessionManager session, Element action) {
		super(session, action);
		_source = requiredAttribute("Path", String.format("%s action requires a Path to a directory or file.", _actionName));
		_countMessage = "deleted";
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
			_filesProcessed++;
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
