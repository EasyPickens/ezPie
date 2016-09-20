package com.fanniemae.devtools.pie.actions.xmltransforms;

import java.io.File;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.StringUtilities;

public abstract class XmlTransform {
	
	protected SessionManager _session;
	
	protected Element _action;
	
	protected String _xPath;
	protected String _attributeName;
	protected String _attributeValue;
	protected String _xmlString;

	protected boolean _required;
	protected boolean _isFolder = false;

	public XmlTransform(SessionManager session, Element action, boolean isFolder) {
		_session = session;
		_action = action;
		_isFolder = isFolder;
	}
	
	public abstract Document execute(Document xmlDocument, File file);
	
	protected String optionalAttribute(String attributeName, String defaultValue) {
		String value = _session.getAttribute(_action, attributeName);
		if (StringUtilities.isNullOrEmpty(value)) {
			value = _session.resolveTokens(defaultValue);
		} else {
			_session.addLogMessage("", attributeName, value);
		}
		return value;
	}

	protected String requiredAttribute(String attributeName) {
		String errorMessage = String.format("Missing a value for %s on the %s element.", attributeName, _action.getNodeName());
		return requiredAttribute(attributeName, errorMessage);
	}

	protected String requiredAttribute(String attributeName, String errorMessage) {
		String value = _session.getAttribute(_action, attributeName);
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
