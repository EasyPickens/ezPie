/**
 *  
 * Copyright (c) 2015 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPIE/wiki/Fannie-Mae-Open-Source-Licensing-Project
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.devtools.pie.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @author Tyler Femenella
 * @since 2015-12-15
 * 
 */

public final class XmlUtilities {
	
	private XmlUtilities() {
	}

	public static Document CreateXMLDocument(String xmlString) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			return builder.parse(new InputSource(new StringReader(xmlString)));
		} catch (ParserConfigurationException | SAXException | IOException ex) {
			return null;
		}
	}

	public static void SaveXmlDocument(String filename, Document xmlDoc) {
		
//		String xmlString = XMLDocumentToString(xmlDoc);
//		FileUtilities.writeFile(filename, xmlString);
		
		try {
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			//transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

			transformer.transform(new DOMSource(xmlDoc), new StreamResult(new FileOutputStream(filename)));
		} catch (IllegalArgumentException | TransformerException | FileNotFoundException ex) {
			throw new RuntimeException("Error saving XML document. " + ex.getMessage(), ex);
		}
	}
	
	public static String XMLDocumentToString(Document doc) {
		try {
			StringWriter sw = new StringWriter();
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

			transformer.transform(new DOMSource(doc), new StreamResult(sw));
			return sw.toString();
		} catch (IllegalArgumentException | TransformerException ex) {
			throw new RuntimeException("Error converting XML document to string. " + ex.getMessage(), ex);
		}
	}

	public static String getOuterXml(Node node) {
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty("omit-xml-declaration", "yes");

			StringWriter writer = new StringWriter();
			transformer.transform(new DOMSource(node), new StreamResult(writer));
			return writer.toString();
		} catch (IllegalArgumentException | TransformerException ex) {
			throw new RuntimeException("Error converting XML node to string. " + ex.getMessage(), ex);
		}
	}

	public static String getInnerXml(Node node) {
		DOMImplementationLS lsImpl = (DOMImplementationLS) node.getOwnerDocument().getImplementation().getFeature("LS", "3.0");
		LSSerializer lsSerializer = lsImpl.createLSSerializer();
		NodeList childNodes = node.getChildNodes();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < childNodes.getLength(); i++) {
			sb.append(lsSerializer.writeToString(childNodes.item(i)));
		}
		return sb.toString();
	}

	public static Node selectSingleNode(Node node, String sXPath) {
		Object oNode = evaluateXPath(node, sXPath, XPathConstants.NODE);
		if (oNode != null) {
			return (Node) oNode;
		}
		return null;
	}

	public static NodeList selectNodes(Node node, String sXPath) {
		Object oNodeList = evaluateXPath(node, sXPath, XPathConstants.NODESET);
		if (oNodeList != null) {
			return (NodeList) oNodeList;
		}
		return null;
	}

	protected static Object evaluateXPath(Node node, String sXPath, QName mode) {
		try {
			XPathFactory xFactory = XPathFactory.newInstance();
			XPath xp = xFactory.newXPath();
			XPathExpression expr = xp.compile(sXPath);
			return expr.evaluate(node, mode);
		} catch (XPathExpressionException ex) {
			return null;
		}
	}

	public static boolean containsChildWithAttribute(Node nodeParent, String searchAttributeName, String searchValue, boolean isCaseInsensitive) {
		if ((nodeParent == null) || (nodeParent.getChildNodes().getLength() == 0)) {
			return false;
		}

		NodeList nl = nodeParent.getChildNodes();
		int iLength = nl.getLength();
		for (int i = 0; i < iLength; i++) {
			if (nl.item(i).getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element xE = (Element) nl.item(i);
			if (xE.getAttribute(searchAttributeName).equals(searchValue)) {
				return true;
			}
			if (isCaseInsensitive && xE.getAttribute(searchAttributeName).equalsIgnoreCase(searchValue)) {
				return true;
			}
		}
		return false;
	}

	public static Element getChildWithAttribute(Node nodeParent, String searchAttributeName, String searchValue, boolean isCaseInsensitive) {
		if ((nodeParent == null) || (nodeParent.getChildNodes().getLength() == 0)) {
			return null;
		}

		NodeList nl = nodeParent.getChildNodes();
		int iLength = nl.getLength();
		for (int i = 0; i < iLength; i++) {
			Element xE = (Element) nl.item(i);
			if (xE.getAttribute(searchAttributeName).equals(searchValue)) {
				return xE;
			}
			if (isCaseInsensitive && xE.getAttribute(searchAttributeName).equalsIgnoreCase(searchValue)) {
				return xE;
			}
		}
		return null;
	}

	public static Document loadXmlDefinition(String filename) {
		ArrayList<String> breadCrumbs = new ArrayList<String>();
		return loadXmlDefinition(filename, null, breadCrumbs);
	}

	public static Document loadXmlFile(String filename) {
		return loadXmlDocument(filename);
	}

	public static Document loadXmlDocument(String filename) {
		if (FileUtilities.isInvalidFile(filename)) {
			throw new RuntimeException(filename + " not found.");
		}
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			return builder.parse(filename);
		} catch (ParserConfigurationException | SAXException | IOException ex) {
			throw new RuntimeException(String.format("Could not load %s XML file.  %s", filename, ex.getMessage()), ex);
		}
	}

	protected static Document loadXmlDefinition(String filename, String sharedElementID, ArrayList<String> breadCrumbs) {
		Document xDoc = loadXmlFile(filename);
		removeRemarkedElements(xDoc.getDocumentElement());

		String xpath = (sharedElementID == null) ? "//ImportSharedElement" : String.format("//SharedElement[@ID='%s']/ImportSharedElement", sharedElementID);
		NodeList nl = selectNodes(xDoc.getDocumentElement(), xpath);

		int length = nl.getLength();
		if (length > 0) {
			List<Element[]> elementsToInsert = new ArrayList<>();
			for (int i = 0; i < length; i++) {
				@SuppressWarnings("unchecked")
				ArrayList<String> loopCrumbs = (ArrayList<String>) breadCrumbs.clone();
				Element ele = (Element) nl.item(i);
				String definitionName = ele.getAttribute("DefinitionName");
				String definitionFilename = definitionName;
				String elementID = ele.getAttribute("SharedElementID");
				if (definitionName.isEmpty()) {
					definitionName = filename;
					definitionFilename = definitionName;
				}
				if (elementID.isEmpty()) {
					throw new RuntimeException("The ImportSharedElement is missing a value in the required SharedElementID");
				}
				if (!definitionFilename.toLowerCase().endsWith(".xml")) {
					definitionFilename += ".xml";
				}
				if (!exists(definitionFilename)) {
					File temp = new File(filename);
					String absolutePath = temp.getAbsolutePath();
					String definitionPath = absolutePath.substring(0, absolutePath.lastIndexOf(File.separator) + 1);
					definitionFilename = definitionPath + definitionFilename;
				}
				if (!exists(definitionFilename)) {
					throw new RuntimeException(String.format("ImportSharedElement could not find the %s referenced definition", definitionName));
				}

				String crumb = String.format("%s|%s", definitionFilename, elementID);
				if (loopCrumbs.indexOf(crumb) > -1) {
					throw new RuntimeException(String.format("Circular ImportSharedElement reference detected. SharedElementID %s in definition %s triggered the error.", elementID, definitionName));
				}
				loopCrumbs.add(crumb);

				Document innerDocument = loadXmlDefinition(definitionFilename, elementID, loopCrumbs);

				// Look for the shared element
				Node sharedNode = XmlUtilities.selectSingleNode(innerDocument, String.format("//SharedElement[@ID='%s']", elementID));
				if (sharedNode == null) {
					throw new RuntimeException(String.format("Could not find a SharedElement with ID=%s referenced in %s", elementID, definitionName));
				}
				NodeList sharedSteps = XmlUtilities.selectNodes(sharedNode, "*");
				int sharedStepsLength = sharedSteps.getLength();
				if (sharedStepsLength > 0) {
					for (int x = 0; x < sharedStepsLength; x++) {
						Element[] elementUpdate = new Element[2];
						elementUpdate[0] = ele;
						elementUpdate[1] = (Element) (sharedSteps.item(x));
						elementsToInsert.add(elementUpdate);
					}
				}
			}
			if (elementsToInsert.size() > 0) {
				// Insert referenced elements
				int insertsToDo = elementsToInsert.size();
				for (int i = 0; i < insertsToDo; i++) {
					Element parent = elementsToInsert.get(i)[0];
					Element newElement = elementsToInsert.get(i)[1];

					parent.getParentNode().insertBefore(xDoc.adoptNode(newElement.cloneNode(true)), parent);
				}
				// Remove ImportSharedElements
				removeElements(xDoc, "//ImportSharedElement");
				// Remove left over empty text nodes
			}
		}
		removeWhitespace(xDoc);
		return xDoc;
	}

	public static void removeRemarkedElements(Element ele) {
		removeElements(ele.getOwnerDocument(), "//Remark");
	}

	protected static void removeElements(Document doc, String xpath) {
		Node emptyTextNode = XmlUtilities.selectSingleNode(doc, xpath);
		while (emptyTextNode != null) {
			emptyTextNode.getParentNode().removeChild(emptyTextNode);
			emptyTextNode = XmlUtilities.selectSingleNode(doc, xpath);
		}
	}

	protected static void removeWhitespace(Document doc) {
		removeElements(doc, "//text()[normalize-space(.)='']");
	}

	protected static boolean exists(String filename) {
		if (filename == null)
			return false;
		File f = new File(filename);
		return f.exists() && f.isFile();
	}

}
