/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.actions.xmltransforms.AppendChild;
import com.fanniemae.ezpie.actions.xmltransforms.InsertAfter;
import com.fanniemae.ezpie.actions.xmltransforms.InsertBefore;
import com.fanniemae.ezpie.actions.xmltransforms.NodeExists;
import com.fanniemae.ezpie.actions.xmltransforms.SaveFile;
import com.fanniemae.ezpie.actions.xmltransforms.SetAttribute;
import com.fanniemae.ezpie.actions.xmltransforms.XmlTransform;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.ReportBuilder;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.common.XmlUtilities;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-08-19
 * 
 */

public class XmlEdit extends Action {

	protected String _xmlString;
	protected String _inputFilename;
	protected String _sourceFolder;
	protected String _outputFilename;
	protected String _xPath;
	protected String _attributeName;
	protected String _newValue;

	protected Document _xmlDoc;

	protected boolean _subFolders = false;
	protected boolean _continueOnError = false;
	protected boolean _isFolder = false;

	protected int _filesFound = 0;
	protected int _filesModified = 0;

	public XmlEdit(SessionManager session, Element action) {
		super(session, action, false);
		_xmlString = optionalAttribute("XmlString", "");
		_inputFilename = optionalAttribute("Filename", "");
		_sourceFolder = optionalAttribute("Folder", "");
		_continueOnError = StringUtilities.toBoolean(optionalAttribute("OnErrorContinue", "False"), false);

		if (isNotNullOrEmpty(_xmlString)) {
			_xmlDoc = XmlUtilities.CreateXMLDocument(_xmlString);
		} else if (isNotNullOrEmpty(_inputFilename)) {
			_xmlDoc = XmlUtilities.loadXmlDocument(_inputFilename);
		} else if (isNotNullOrEmpty(_sourceFolder) && FileUtilities.isValidDirectory(_sourceFolder)) {
			// Working on all the XML Files within a folder - built for Informatica and CAST version issue.
			_isFolder = true;
		} else {
			throw new RuntimeException("XmlEdit requires either a value in XmlString or a Filename to an XML file.");
		}
	}

	@Override
	public String executeAction() {
		// Setup XmlTransformations
		List<XmlTransform> xmlTransforms = new ArrayList<XmlTransform>();
		NodeList nlEdits = XmlUtilities.selectNodes(_action, "*");
		int length = nlEdits.getLength();
		if (length == 0)
			return "";

		for (int i = 0; i < length; i++) {
			Element xmlEdit = (Element) (nlEdits.item(i));
			String nodeName = xmlEdit.getNodeName();
			_session.addLogMessage(nodeName, "Setting Up", "Perparing XmlTransforms");
			switch (nodeName) {
			case "SetAttribute":
				xmlTransforms.add(new SetAttribute(_session, xmlEdit, _isFolder));
				break;
			case "AppendChild":
				xmlTransforms.add(new AppendChild(_session, xmlEdit, _isFolder));
				break;
			case "InsertBefore":
				xmlTransforms.add(new InsertBefore(_session, xmlEdit, _isFolder));
				break;
			case "InsertAfter":
				xmlTransforms.add(new InsertAfter(_session, xmlEdit, _isFolder));
				break;
			case "NodeExists":
				xmlTransforms.add(new NodeExists(_session, xmlEdit, _isFolder));
				break;
			case "SaveFile":
				xmlTransforms.add(new SaveFile(_session, xmlEdit, _isFolder));
				break;
			default:
				_session.addLogMessage("** Warning **", nodeName, "XmlEdit does not currently support this processing step.");
			}
		}

		File[] files = null;
		File sourceFolder = new File(_sourceFolder);
		files = sourceFolder.listFiles();

		Document xmlDoc = null;
		int lengthTransforms = xmlTransforms.size();
		if (_isFolder) {
			ReportBuilder rb = new ReportBuilder();
			for (int i = 0; i < files.length; i++) {
				_filesFound++;
				String filename = files[i].getAbsolutePath();
				if (files[i].isFile() && files[i].getName().toLowerCase().endsWith(".xml")) {
					rb.appendFormatLine("%,d Modifying: %s", _filesFound, filename);
					xmlDoc = XmlUtilities.loadXmlDocument(filename);
					for (int x = 0; x < lengthTransforms; x++) {
						xmlDoc = xmlTransforms.get(x).execute(xmlDoc, files[i]);
					}
					_filesModified++;
				} else {
					rb.appendFormatLine("%,d Skipping: %s", _filesFound, filename);
				}
			}
			_session.addLogMessage("", "Files Transformed", "View Report", "file://"+FileUtilities.writeRandomTextFile(_session.getLogPath(), rb.toString()));
		} else {
			if (isNotNullOrEmpty(_xmlString)) {
				xmlDoc = XmlUtilities.CreateXMLDocument(_xmlString);
			} else if (isNotNullOrEmpty(_inputFilename)) {
				xmlDoc = XmlUtilities.loadXmlDocument(_inputFilename);
			} else {
				throw new RuntimeException("XmlEdit requires either a value in XmlString, Filename, or path to an XML file.");
			}

			_filesFound++;
			for (int x = 0; x < lengthTransforms; x++) {
				xmlDoc = xmlTransforms.get(x).execute(xmlDoc, null);
			}
			_filesModified++;
		}
		_session.addLogMessage("", "File Count", String.format("%,d files modified out of %,d files found.",_filesModified,_filesFound));

		// NodeList nlEdits = XmlUtilities.selectNodes(_action, "*");
		// int length = nlEdits.getLength();
		// if (length == 0)
		// return "";
		//
		// for (int i = 0; i < length; i++) {
		// Element xmlEdit = (Element) (nlEdits.item(i));
		// String nodeName = xmlEdit.getNodeName();
		// _session.addLogMessage(nodeName, "Process", "Processing XML edit action.");
		// switch (nodeName) {
		// case "SetAttribute":
		// setAttribute(xmlEdit);
		// break;
		// case "AppendChild":
		// appendChild(xmlEdit);
		// break;
		// case "InsertBefore":
		// insertBefore(xmlEdit);
		// break;
		// case "InsertAfter":
		// insertAfter(xmlEdit);
		// break;
		// case "SaveFile":
		// saveFile(xmlEdit);
		// break;
		// default:
		// _session.addLogMessage("** Warning **", nodeName, "XmlEdit does not currently support this processing step.");
		// }
		// }
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
		} else if (!required && (length == 0)) {
			return;
		}

		for (int i = 0; i < length; i++) {
			Element ele = (Element) nl.item(i);
			ele.setAttribute(attributeName, attributeValue);
		}
	}

	protected void appendChild(Element xmlEdit) {
		String xPath = optionalAttribute(xmlEdit, "XPath", "");
		String xmlString = requiredAttribute(xmlEdit, "XmlString");
		Boolean required = StringUtilities.toBoolean(optionalAttribute(xmlEdit, "Required", ""), true);

		Document tempDoc = XmlUtilities.CreateXMLDocument(String.format("<temp>%s</temp>", xmlString));
		NodeList nlNew = XmlUtilities.selectNodes(tempDoc.getDocumentElement(), "*");
		int length = nlNew.getLength();
		if (required && (length == 0)) {
			throw new RuntimeException("XmlString does not contain any nodes to append.");
		} else if (length == 0) {
			_session.addLogMessage("", "** Warning **", "XmlString does not contain any nodes to append.");
			return;
		}

		NodeList targetNodes = null;
		Node targetNode = null;
		if (StringUtilities.isNullOrEmpty(xPath)) {
			targetNode = _xmlDoc.getDocumentElement();
			for (int i = 0; i < length; i++) {
				targetNode.appendChild(_xmlDoc.adoptNode(nlNew.item(i).cloneNode(true)));
			}
		} else {
			targetNodes = XmlUtilities.selectNodes(_xmlDoc, xPath);
			if (required && (targetNodes.getLength() == 0)) {
				throw new RuntimeException(String.format("%s did not return any matching nodes.", xPath));
			}
			int targetLength = targetNodes.getLength();
			for (int x = 0; x < targetLength; x++) {
				targetNode = targetNodes.item(x);
				for (int i = 0; i < length; i++) {
					targetNode.appendChild(_xmlDoc.adoptNode(nlNew.item(i).cloneNode(true)));
				}
			}
		}
	}

	protected void insertBefore(Element xmlEdit) {
		String xPath = requiredAttribute(xmlEdit, "XPath");
		String xmlString = requiredAttribute(xmlEdit, "XmlString");
		Boolean required = StringUtilities.toBoolean(optionalAttribute(xmlEdit, "Required", ""), true);

		Document tempDoc = XmlUtilities.CreateXMLDocument(String.format("<temp>%s</temp>", xmlString));
		NodeList nlNew = XmlUtilities.selectNodes(tempDoc.getDocumentElement(), "*");
		int length = nlNew.getLength();
		if (required && (length == 0)) {
			throw new RuntimeException("XmlString does not contain any nodes to append.");
		} else if (length == 0) {
			_session.addLogMessage("", "** Warning **", "XmlString does not contain any nodes to append.");
			return;
		}

		Node targetNode = XmlUtilities.selectSingleNode(_xmlDoc, xPath);
		if (required && (targetNode == null)) {
			throw new RuntimeException(String.format("%s did not return a matching node.", xPath));
		}
		for (int i = 0; i < length; i++) {
			targetNode.getParentNode().insertBefore(_xmlDoc.adoptNode(nlNew.item(i).cloneNode(true)), targetNode);
		}
	}

	protected void insertAfter(Element xmlEdit) {
		String xPath = requiredAttribute(xmlEdit, "XPath");
		String xmlString = requiredAttribute(xmlEdit, "XmlString");
		Boolean required = StringUtilities.toBoolean(optionalAttribute(xmlEdit, "Required", ""), true);

		Document tempDoc = XmlUtilities.CreateXMLDocument(String.format("<temp>%s</temp>", xmlString));
		NodeList nlNew = XmlUtilities.selectNodes(tempDoc.getDocumentElement(), "*");
		int length = nlNew.getLength();
		if (required && (length == 0)) {
			throw new RuntimeException("XmlString does not contain any nodes to append.");
		} else if (length == 0) {
			_session.addLogMessage("", "** Warning **", "XmlString does not contain any nodes to append.");
			return;
		}

		Node targetNode = XmlUtilities.selectSingleNode(_xmlDoc, xPath);
		if (required && (targetNode == null)) {
			throw new RuntimeException(String.format("%s did not return a matching node.", xPath));
		}
		Node insertPoint = targetNode.getNextSibling();
		for (int i = 0; i < length; i++) {
			targetNode.getParentNode().insertBefore(_xmlDoc.adoptNode(nlNew.item(i).cloneNode(true)), insertPoint);
		}
	}

	protected void saveFile(Element xmlEdit) {
		String randomFilename = FileUtilities.getRandomFilename(_session.getStagingPath(), "xml");
		String filename = optionalAttribute(xmlEdit, "Filename", randomFilename);
		String tokenName = optionalAttribute(xmlEdit, "Name", "");
		XmlUtilities.SaveXmlDocument(filename, _xmlDoc);
		String xmlLogCopy = FileUtilities.writeRandomFile(_session.getLogPath(), "txt", XmlUtilities.XMLDocumentToString(_xmlDoc));
		if (StringUtilities.isNotNullOrEmpty(tokenName)) {
			_session.addToken("LocalData", tokenName, filename);
		}
		_session.addLogMessage("", "File Saved", "View Modified Xml", "file://" + xmlLogCopy);
	}

}
