package com.fanniemae.devtools.pie.actions;

import java.io.File;

/**
 * 
 * @author Richard Monson
 * @since 2016-12-10
 * 
 * <DeleteEmpty Path="" />
 */

import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.SessionManager;

public class DeleteEmpty extends Delete {

	public DeleteEmpty(SessionManager session, Element action) {
		super(session, action);
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
			if (sourceFile.length() == 0) {
				sourceFile.delete();
				_filesProcessed++;
			}
		} catch (Exception e) {
			RuntimeException ex = new RuntimeException(String.format("Error while trying to delete %s. Message is %s", source, e.getMessage()), e);
			throw ex;
		}
	}
}
