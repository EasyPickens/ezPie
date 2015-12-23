package com.fanniemae.automation.actions;

import org.w3c.dom.Element;
import com.fanniemae.automation.SessionManager;
import com.fanniemae.automation.common.StringUtilities;

/**
*
* @author Richard Monson
* @since 2015-12-16
* 
*/
public abstract class Action {

	protected SessionManager _Session;
	protected Element _Action;
	
	protected String _ID;
	
	public Action(SessionManager session, Element eleAction) {
		_Session = session;
		_Action = eleAction;
		_ID = _Session.getAttribute(eleAction, "ID");
		
		if (StringUtilities.isNullOrEmpty(_ID))
			throw new RuntimeException(String.format("%s is missing a required ID value.", _Action.getNodeName()));
		_Session.addLogMessage(_Action.getNodeName(), "ID", _ID);
	}
	
	public abstract String execute();

}
