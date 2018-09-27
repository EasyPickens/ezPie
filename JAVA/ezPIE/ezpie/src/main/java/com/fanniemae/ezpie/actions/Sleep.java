/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.actions;

import java.util.HashMap;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.StringUtilities;

/**
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2016-07-08
 * 
 * NOTE: Purely for testing and debugging.
 * 
 */

public class Sleep extends Action {
	
	int _seconds;

	public Sleep(SessionManager session, Element action) {
		super(session, action, false);
		
		_seconds = StringUtilities.toInteger(optionalAttribute("Seconds", "30"));
	}

	@Override
	public String executeAction(HashMap<String, String> dataTokens) {
		_session.setDataTokens(dataTokens);
		_session.addLogMessage("", "Time", String.format("Sleeping for %s seconds", _seconds));
		try {
			Thread.sleep(_seconds * 1000L);
		} catch (InterruptedException e) {
			// Fail silent - for debuging only.
		} 
		_session.clearDataTokens();
		return null;
	}

}
