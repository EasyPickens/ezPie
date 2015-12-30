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
	protected String _ActionName;

	public Action(SessionManager session, Element eleAction) {
		_Session = session;
		_Action = eleAction;
		_ID = _Session.getAttribute(eleAction, "ID");
		_ActionName = StringUtilities.isNullOrEmpty(_Action.getAttribute("Type")) ? _Action.getNodeName() : _Action.getNodeName() + "." + _Action.getAttribute("Type");

		String nodeName = "|" + _ActionName + "|";
		if (StringUtilities.isNullOrEmpty(_ID)) {
			if ("|LogComment|LocalTokens|Export.Delimited|".indexOf(nodeName) == -1) {
				throw new RuntimeException(String.format("%s is missing a required ID value.", _Action.getNodeName()));
			} else {
				_Session.addLogMessage(_ActionName, "Process", String.format("Starting to process the %s operation.", _ActionName));
			}
		} else {
			_Session.addLogMessage(_ActionName, "ID", _ID);
		}
	}

	public abstract String execute();

}
