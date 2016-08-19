package com.fanniemae.devtools.pie.actions;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.FileUtilities;
import com.fanniemae.devtools.pie.common.StringUtilities;
import com.fanniemae.devtools.pie.common.XmlUtilities;

public class XmlEdit extends Action {

	protected String _xmlString;
	protected String _inputFilename;
	protected String _outputFilename;
	protected String _xPath;
	protected String _attributeName;
	protected String _newValue;

	protected Document _xmlDoc;

	public XmlEdit(SessionManager session, Element action) {
		super(session, action, false);
		_xmlString = optionalAttribute("XmlString", "");
		_inputFilename = optionalAttribute("Filename", "");

		if (isNotNullOrEmpty(_xmlString)) {
			_xmlDoc = XmlUtilities.CreateXMLDocument(_xmlString);
		} else if (isNotNullOrEmpty(_inputFilename)) {
			_xmlDoc = XmlUtilities.loadXmlDocument(_inputFilename);
		} else {
			throw new RuntimeException("XmlEdit requires either a value in XmlString or a Filename to an XML file.");
		}
	}

	@Override
	public String executeAction() {
		NodeList nlEdits = XmlUtilities.selectNodes(_action, "*");
		int length = nlEdits.getLength();
		if (length == 0)
			return "";

		for (int i = 0; i < length; i++) {
			Element xmlEdit = (Element) (nlEdits.item(i));
			String nodeName = xmlEdit.getNodeName();
			switch (nodeName) {
			case "SetAttribute":
				setAttribute(xmlEdit);
				break;
			case "AppendChild":
				appendChild(xmlEdit);
				break;
			case "SaveFile":
				saveFile(xmlEdit);
				break;
			default:
				_session.addLogMessage("** Warning **", nodeName, "XmlEdit does not currently support this processing step.");
			}
		}
		return null;
	}

	protected void setAttribute(Element xmlEdit) {
		String xPath = requiredAttribute(xmlEdit, "XPath");
		String attributeName = requiredAttribute(xmlEdit, "AttributeName");
		String attributeValue = optionalAttribute(xmlEdit, "AttributeValue", "");
		Boolean required = StringUtilities.toBoolean(optionalAttribute(xmlEdit, "Required", ""), true);

		NodeList nl = XmlUtilities.selectNodes(_xmlDoc, xPath);
		int length = nl.getLength();
		if (required && (length == 0)) {
			throw new RuntimeException(String.format("No matching nodes found for the XPath %s", xPath));
		} else if (!required && (length ==0)) {
			return;
		}
		
		for (int i = 0; i < length; i++) {
			Element ele = (Element) nl.item(i);
			ele.setAttribute(attributeName, attributeValue);
		}
	}
	
	protected void appendChild(Element xmlEdit) {
		String xPath = requiredAttribute(xmlEdit, "XPath");
		String xmlString = requiredAttribute(xmlEdit, "XmlString");
		Boolean required = StringUtilities.toBoolean(optionalAttribute(xmlEdit, "Required", ""), true);
		
		Document temp = XmlUtilities.CreateXMLDocument(String.format("<temp>%s</temp>",xmlString));
		NodeList nlNew = XmlUtilities.selectNodes(temp.getDocumentElement(), "*");
		int length = nlNew.getLength();
		if (length == 0) return;
		
		NodeList nlAppendHere = XmlUtilities.selectNodes(_xmlDoc, xPath);
		
		
		for(int i=0;i<length;i++) {
			
		}
		
//		doc 
//
//		NodeList nl = XmlUtilities.selectNodes(_xmlDoc, xPath);
//		int length = nl.getLength();
//		if (required && (length == 0)) {
//			throw new RuntimeException(String.format("No matching nodes found for the XPath %s", xPath));
//		} else if (!required && (length ==0)) {
//			return;
//		}
//		
//		for (int i = 0; i < length; i++) {
//			Element ele = (Element) nl.item(i);
//			ele.setAttribute(attributeName, attributeValue);
//		}
	}
	
	protected void saveFile(Element xmlEdit) {
		String filename = requiredAttribute(xmlEdit, "Filename");
		XmlUtilities.SaveXmlDocument(filename, _xmlDoc);
		String xmlLogCopy = FileUtilities.writeRandomFile(_session.getLogPath(), ".txt", XmlUtilities.XMLDocumentToString(_xmlDoc));
		_session.addLogMessage("", "File Saved", "View Modified Xml","file://"+xmlLogCopy);
	}

}
