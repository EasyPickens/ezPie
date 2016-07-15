package com.fanniemae.devtools.pie.actions;

import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.SessionManager;

public class Tokens extends Action {

	public Tokens(SessionManager session, Element action) {
		super(session, action, false);
	}

	@Override
	public String execute() {
		_session.addTokens(_action);
		return null;
	}

}
