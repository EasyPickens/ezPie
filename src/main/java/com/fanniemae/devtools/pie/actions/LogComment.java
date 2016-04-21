package com.fanniemae.devtools.pie.actions;

import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.SessionManager;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-29
 * 
 */
public class LogComment extends Action {
	
	public LogComment(SessionManager session, Element action) {
		super(session, action, false);
	}

	@Override
	public String execute() {
		_session.addLogMessage("", "Message", _session.getAttribute(_action, "Message"));
		return "";
	}
}
