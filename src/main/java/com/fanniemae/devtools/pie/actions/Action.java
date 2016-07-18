package com.fanniemae.devtools.pie.actions;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.StringUtilities;

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
			_session.addLogMessage(_actionName, "Process", String.format("Starting to process the %s action.", _actionName));
		} else {
			_session.addLogMessage(_actionName, "ID", _id);
		}
	}

	public abstract String execute();

	protected String optionalAttribute(String attributeName, String defaultValue) {
		return optionalAttribute(_action, attributeName, defaultValue);
	}

	protected String optionalAttribute(Node node, String attributeName, String defaultValue) {
		return optionalAttribute((Element)node, attributeName, defaultValue);
	}
	
	protected String optionalAttribute(Element element, String attributeName, String defaultValue) {
		String value = _session.getAttribute(element, attributeName);
		if (StringUtilities.isNullOrEmpty(value)) {
			value = _session.resolveTokens(defaultValue);
		} else {
			_session.addLogMessage("", attributeName, value);
		}
		return value;
	}

	protected String requiredAttribute(String attributeName) {
		return requiredAttribute(_action, attributeName);
	}

	protected String requiredAttribute(String attributeName, String errorMessage) {
		return requiredAttribute(_action, attributeName, errorMessage);
	}

	protected String requiredAttribute(Node node, String attributeName) {
		return requiredAttribute((Element)node, attributeName);
	}
	
	protected String requiredAttribute(Element element, String attributeName) {
		String errorMessage = String.format("Missing a value for %s on the %s element.", attributeName, element.getNodeName());
		return requiredAttribute(element, attributeName, errorMessage);
	}
	
	protected String requiredAttribute(Node node, String attributeName, String errorMessage) {
		return requiredAttribute((Element)node, attributeName, errorMessage);
	}

	protected String requiredAttribute(Element element, String attributeName, String errorMessage) {
		String value = _session.getAttribute(element, attributeName);
		if (StringUtilities.isNullOrEmpty(value)) {
			throw new RuntimeException(errorMessage);
		}
		_session.addLogMessage("", attributeName, value);
		return value;
	}
}
