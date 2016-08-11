package com.fanniemae.devtools.pie.actions;

import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.StringUtilities;

public class Sleep extends Action {
	
	int _seconds;

	public Sleep(SessionManager session, Element action) {
		super(session, action, false);
		
		_seconds = StringUtilities.toInteger(optionalAttribute("Seconds", "30"));
	}

	@Override
	public String executeAction() {
		_session.addLogMessage("", "Time", String.format("Sleeping for %s seconds", _seconds));
		try {
			Thread.sleep(_seconds * 1000L);
		} catch (InterruptedException e) {
			// Fail silent - for debuging only.
		} 
		return null;
	}

}
