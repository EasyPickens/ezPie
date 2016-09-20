package com.fanniemae.devtools.pie.actions.xmltransforms;

import java.io.File;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.StringUtilities;
import com.fanniemae.devtools.pie.common.XmlUtilities;

public class SetAttribute extends XmlTransform {
	
	public SetAttribute(SessionManager session, Element action, boolean isFolder) {
		super(session, action, isFolder);
		_xPath = requiredAttribute("XPath");
		_attributeName = requiredAttribute("AttributeName");
		_attributeValue = optionalAttribute("AttributeValue", "");
		_required = StringUtilities.toBoolean(optionalAttribute("Required", ""), true);
	}

	@Override
	public Document execute(Document xmlDocument, File file) {
		NodeList nl = XmlUtilities.selectNodes(xmlDocument, _xPath);
		int length = nl.getLength();
		if (_required && (length == 0)) {
			throw new RuntimeException(String.format("No matching nodes found for the XPath %s", _xPath));
		} else if (!_required && (length == 0)) {
			return xmlDocument;
		}

		for (int i = 0; i < length; i++) {
			Element ele = (Element) nl.item(i);
			ele.setAttribute(_attributeName, _attributeValue);
		}
		return xmlDocument;
	}

}
