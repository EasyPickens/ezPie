package com.fanniemae.automation.actions;

import org.w3c.dom.Element;

import com.fanniemae.automation.SessionManager;

/**
*
* @author Richard Monson
* @since 2015-12-21
* 
*/
public class LocalTokens extends Action {

	public LocalTokens(SessionManager session, Element action) {
		super(session, action, false);
	}

	@Override
	public String execute() {
		_Session.addTokens("Local", _Action);
		return "";
	}

}
