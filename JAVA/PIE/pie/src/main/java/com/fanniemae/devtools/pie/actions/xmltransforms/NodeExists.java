package com.fanniemae.devtools.pie.actions.xmltransforms;

import java.io.File;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.XmlUtilities;

public class NodeExists extends XmlTransform {
	
	protected String _id = "NodeExists";

	public NodeExists(SessionManager session, Element action, boolean isFolder) {
		super(session, action, isFolder);
		_xPath = requiredAttribute("XPath");
		_id = requiredAttribute("ID");
	}

	@Override
	public Document execute(Document xmlDocument, File file) {
		NodeList targetNodes = XmlUtilities.selectNodes(xmlDocument, _xPath);
		if ((targetNodes == null) || (targetNodes.getLength() == 0)) {
			_session.addToken("NodeExists", _id, "false");
		} else {
			_session.addToken("NodeExists", _id, "true");
		}
		return xmlDocument;
	}

}
