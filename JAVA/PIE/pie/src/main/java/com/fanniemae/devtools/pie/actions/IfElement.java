package com.fanniemae.devtools.pie.actions;

import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.ScriptUtilities;

public class IfElement extends Action {
	
	protected String _expression = "false";

	public IfElement(SessionManager session, Element action) {
		super(session, action, false);
		
		_expression = requiredAttribute("Expression");
	}
	
	public boolean evalToBoolean() {
		Boolean result = ScriptUtilities.evalToBoolean(_expression);
		_session.addLogMessage("", "Result", result.toString());
		return result;
	}

	@Override
	public String executeAction() {
		// Element is used to control definition branching, no actual action.
		return "";
	}

}
