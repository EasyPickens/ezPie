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

	protected SessionManager _session;
	protected Element _action;

	protected String _id;
	protected String _actionType;
	protected String _actionName;
	
	protected boolean _idRequired = true;

	public Action(SessionManager session, Element action) {
		this(session, action, true);
	}
	
	public Action(SessionManager session, Element action, Boolean idRequired) {
		_session = session;
		_action = action;
		_idRequired = idRequired;

		_id = _session.getAttribute(_action, "ID");
		_actionType = _action.getAttribute("Type");
		_actionName = StringUtilities.isNullOrEmpty(_actionType) ? _action.getNodeName() : _action.getNodeName() + "." + _actionType;

		if (_idRequired && StringUtilities.isNullOrEmpty(_id)) {
			throw new RuntimeException(String.format("%s is missing a required ID value.", _action.getNodeName()));
		} else if (StringUtilities.isNullOrEmpty(_id)) {
			_session.addLogMessage(_actionName, "Process", String.format("Starting to process the %s operation.", _actionName));
		} else {
			_session.addLogMessage(_actionName, "ID", _id);
		}
	}

	public abstract String execute();
	
}
