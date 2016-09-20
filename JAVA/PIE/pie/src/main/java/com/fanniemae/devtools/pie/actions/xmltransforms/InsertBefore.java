package com.fanniemae.devtools.pie.actions.xmltransforms;

import java.io.File;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.StringUtilities;
import com.fanniemae.devtools.pie.common.XmlUtilities;

public class InsertBefore extends XmlTransform {

	public InsertBefore(SessionManager session, Element action, boolean isFolder) {
		super(session, action, isFolder);
		_xPath = requiredAttribute("XPath");
		_xmlString = requiredAttribute("XmlString");
		_required = StringUtilities.toBoolean(optionalAttribute("Required", ""), true);
	}

	@Override
	public Document execute(Document xmlDocument, File file) {
		Document tempDoc = XmlUtilities.CreateXMLDocument(String.format("<temp>%s</temp>", _xmlString));
		NodeList nlNew = XmlUtilities.selectNodes(tempDoc.getDocumentElement(), "*");
		int length = nlNew.getLength();
		if (_required && (length == 0)) {
			throw new RuntimeException("XmlString does not contain any nodes to append.");
		} else if (length == 0) {
			_session.addLogMessage("", "** Warning **", "XmlString does not contain any nodes to append.");
			return xmlDocument;
		}

		Node targetNode = XmlUtilities.selectSingleNode(xmlDocument, _xPath);
		if (_required && (targetNode == null)) {
			throw new RuntimeException(String.format("%s did not return a matching node.", _xPath));
		}
		for (int i = 0; i < length; i++) {
			targetNode.getParentNode().insertBefore(xmlDocument.adoptNode(nlNew.item(i).cloneNode(true)),targetNode);
		}
		return xmlDocument;
	}

}
