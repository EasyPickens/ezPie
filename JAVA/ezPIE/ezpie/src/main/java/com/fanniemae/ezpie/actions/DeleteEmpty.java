/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.actions;

import java.io.File;

/**
 * 
 * @author Richard Monson
 * @since 2016-12-10
 * 
 * <DeleteEmpty Path="" />
 */

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.PieException;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-11-22
 * 
 */

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
			throw new PieException(String.format("Error while trying to delete %s. Message is %s", source, e.getMessage()), e);
		}
	}
}
