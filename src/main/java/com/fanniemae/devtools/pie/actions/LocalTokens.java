package com.fanniemae.devtools.pie.actions;

import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.SessionManager;

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
	public String executeAction() {
		_session.addTokens("Local", _action);
		return "";
	}

}
