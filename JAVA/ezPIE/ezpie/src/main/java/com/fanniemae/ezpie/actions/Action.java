/**
 *  
 * Copyright (c) 2015 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
 *
 */

package com.fanniemae.ezpie.actions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.DateUtilities;
import com.fanniemae.ezpie.common.StringUtilities;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2015-12-23
 *
 */

public abstract class Action {

	protected SessionManager _session;
	protected Element _action;

	protected String _name;
	protected String _actionType;
	protected String _actionName;

	protected boolean _idRequired = true;
	protected SimpleDateFormat _sdf = new SimpleDateFormat("MMMM d, yyyy HH:mm:ss");

	protected long _start;
	
	protected HashMap<String, String> _dataTokens = null;

	public Action(SessionManager session, Element action) {
		this(session, action, true);
	}

	public Action(SessionManager session, Element action, Boolean idRequired) {
		_session = session;
		_action = action;
		_idRequired = idRequired;

		_name = _session.getAttribute(_action, "Name");
		_actionType = _action.getAttribute("Type");
		_actionName = StringUtilities.isNullOrEmpty(_actionType) ? _action.getNodeName() : _action.getNodeName() + "." + _actionType;

		if (_idRequired && StringUtilities.isNullOrEmpty(_name)) {
			throw new RuntimeException(String.format("%s is missing a required Name value.", _action.getNodeName()));
		}

		if (!"Log".equals(_actionName)) {
			_session.addLogMessage(_actionName, "Process", String.format("Processing %s action (started: %s)", _actionName, _sdf.format(new Date())));
		}
		_start = System.currentTimeMillis();
		if (StringUtilities.isNotNullOrEmpty(_name)) {
			_session.addLogMessage("", "Name", _name);
		}
	}

	public String execute(HashMap<String, String> dataTokens) {
		String result = executeAction(dataTokens);
		if (!"Log".equals(_actionName) && !"If".equals(_actionName)) {
			_session.addLogMessage("", String.format("%s Completed", _actionName), String.format("Elapsed time: %s", DateUtilities.elapsedTime(_start)));
		}
		return result;
	}

	public abstract String executeAction(HashMap<String, String> dataTokens);

	protected String optionalAttribute(String attributeName, String defaultValue) {
		return optionalAttribute(_action, attributeName, defaultValue);
	}

	protected String optionalAttribute(Node node, String attributeName, String defaultValue) {
		return optionalAttribute((Element) node, attributeName, defaultValue);
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
		return requiredAttribute((Element) node, attributeName);
	}

	protected String requiredAttribute(Element element, String attributeName) {
		String errorMessage = String.format("Missing a value for %s on the %s element.", attributeName, element.getNodeName());
		return requiredAttribute(element, attributeName, errorMessage);
	}

	protected String requiredAttribute(Node node, String attributeName, String errorMessage) {
		return requiredAttribute((Element) node, attributeName, errorMessage);
	}

	protected String requiredAttribute(Element element, String attributeName, String errorMessage) {
		String value = _session.getAttribute(element, attributeName);
		if (StringUtilities.isNullOrEmpty(value)) {
			throw new RuntimeException(errorMessage);
		}
		_session.addLogMessage("", attributeName, value);
		return value;
	}

	protected boolean isNotNullOrEmpty(String value) {
		return !isNullOrEmpty(value);
	}

	protected boolean isNullOrEmpty(String value) {
		return StringUtilities.isNullOrEmpty(value);
	}
}
