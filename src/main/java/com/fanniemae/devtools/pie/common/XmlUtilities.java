package com.fanniemae.devtools.pie.common;

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
 * @author Richard Monson
 * @since 2015-12-15
 * 
 */
public class XmlUtilities {

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

	public static String XMLDocumentToString(Document doc) {
		try {
			StringWriter sw = new StringWriter();
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

			transformer.transform(new DOMSource(doc), new StreamResult(sw));
			return sw.toString();
		} catch (IllegalArgumentException | TransformerException ex) {
			throw new RuntimeException("Error converting to String", ex);
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
			throw new RuntimeException("Error converting to String", ex);
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
		Document xDoc = loadXmlDocument(filename);

		// Remove remarked elements
		List<Node> aRemarks = new ArrayList<>();

		NodeList nl = selectNodes(xDoc.getDocumentElement(), "//Remark | //Note | //*[@Remark] | //Comment");
		if (nl.getLength() == 0) {
			return xDoc;
		}

		int iLength = nl.getLength();
		for (int i = 0; i < iLength; i++) {
			aRemarks.add(nl.item(i));
		}

		iLength = aRemarks.size();
		for (int i = 0; i < iLength; i++) {
			aRemarks.get(i).getParentNode().removeChild(aRemarks.get(i));
		}
		return xDoc;
	}

	public static Document loadXmlDocument(String filename) {
		if (FileUtilities.isInvalidFile(filename)) {
			throw new RuntimeException(filename + " not found.");
		}
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			return builder.parse(filename);
		} catch (ParserConfigurationException | SAXException | IOException ex) {
			throw new RuntimeException(ex.getMessage(), ex);
		}
	}
}
