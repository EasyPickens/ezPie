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
	protected String _ActionType;
	protected String _ActionName;
	protected static String _IDOptional = "|LogComment|LocalTokens|Export.Delimited|SvnCheckout|";
	
	protected Boolean _IDRequired = true;

	public Action(SessionManager session, Element eleAction) {
		this(session, eleAction, true);
	}
	
	public Action(SessionManager session, Element eleAction, Boolean bIDRequired) {
		_Session = session;
		_Action = eleAction;
		_IDRequired = bIDRequired;

		_ID = _Session.getAttribute(_Action, "ID");
		_ActionType = _Action.getAttribute("Type");
		_ActionName = StringUtilities.isNullOrEmpty(_ActionType) ? _Action.getNodeName() : _Action.getNodeName() + "." + _ActionType;

		if (_IDRequired && StringUtilities.isNullOrEmpty(_ID)) {
			throw new RuntimeException(String.format("%s is missing a required ID value.", _Action.getNodeName()));
		} else if (StringUtilities.isNullOrEmpty(_ID)) {
			_Session.addLogMessage(_ActionName, "Process", String.format("Starting to process the %s operation.", _ActionName));
		} else {
			_Session.addLogMessage(_ActionName, "ID", _ID);
		}
	}

	public abstract String execute();
	
}
