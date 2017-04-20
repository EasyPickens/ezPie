/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPIE/wiki/Fannie-Mae-Open-Source-Licensing-Project
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
**/

package com.fanniemae.devtools.pie.actions.xmltransforms;

import java.io.File;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.StringUtilities;
import com.fanniemae.devtools.pie.common.XmlUtilities;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-09-20
 * 
 */

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
