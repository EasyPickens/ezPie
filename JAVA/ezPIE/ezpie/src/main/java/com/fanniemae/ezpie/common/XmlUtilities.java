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

package com.fanniemae.ezpie.common;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
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
	private final static String XPATH_SELECT_WHITESPACE = "//text()[normalize-space(.)='']";
	private final static String XPATH_SELECT_REMARKS = "//Remark";
	private final static String XML_SET_INDENT_PROPERTY = "{http://xml.apache.org/xslt}indent-amount";

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

	public static Document createXMLDocument(String xmlString) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			return builder.parse(new InputSource(new StringReader(xmlString)));
		} catch (ParserConfigurationException | SAXException | IOException ex) {
			return null;
		}
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

	public static Document loadXmlFile(String filename) {
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

	public static void removeElements(Document doc, String xpath) {
		Node emptyTextNode = XmlUtilities.selectSingleNode(doc, xpath);
		while (emptyTextNode != null) {
			emptyTextNode.getParentNode().removeChild(emptyTextNode);
			emptyTextNode = XmlUtilities.selectSingleNode(doc, xpath);
		}
	}

	public static void removeRemarkedElements(Element ele) {
		removeElements(ele.getOwnerDocument(), XPATH_SELECT_REMARKS);
	}

	public static void removeWhitespace(Document doc) {
		removeElements(doc, XPATH_SELECT_WHITESPACE);
	}

	public static void saveXmlFile(String filename, Document doc) {
		try {
			Source src = new DOMSource(doc);
			Result result = new StreamResult(new FileOutputStream(filename));
			// Remove white spaces outside tags, required to update indents
			doc.normalize();
			XPath xPath = XPathFactory.newInstance().newXPath();
			NodeList nodeList = (NodeList) xPath.evaluate(XPATH_SELECT_WHITESPACE, doc, XPathConstants.NODESET);

			for (int i = 0; i < nodeList.getLength(); ++i) {
				Node node = nodeList.item(i);
				node.getParentNode().removeChild(node);
			}

			Transformer transform = TransformerFactory.newInstance().newTransformer();
			transform.setOutputProperty(OutputKeys.INDENT, "yes");
			transform.setOutputProperty(XML_SET_INDENT_PROPERTY, "3");
			transform.transform(src, result);
		} catch (IllegalArgumentException | TransformerException | XPathExpressionException ex) {
			throw new RuntimeException("Error saving XML document. " + ex.getMessage(), ex);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Error saving XML document. " + e.getMessage(), e);
		}
	}

	public static NodeList selectNodes(Node node, String sXPath) {
		Object oNodeList = evaluateXPath(node, sXPath, XPathConstants.NODESET);
		if (oNodeList != null) {
			return (NodeList) oNodeList;
		}
		return null;
	}

	public static Node selectSingleNode(Node node, String sXPath) {
		Object oNode = evaluateXPath(node, sXPath, XPathConstants.NODE);
		if (oNode != null) {
			return (Node) oNode;
		}
		return null;
	}

	public static String xmlDocumentToString(Document doc) {
		try {
			StringWriter sw = new StringWriter();
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(XML_SET_INDENT_PROPERTY, "3");

			transformer.transform(new DOMSource(doc), new StreamResult(sw));
			return sw.toString();
		} catch (IllegalArgumentException | TransformerException ex) {
			throw new RuntimeException("Error converting XML document to string. " + ex.getMessage(), ex);
		}
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

	private XmlUtilities() {
	}
}
