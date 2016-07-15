package com.fanniemae.devtools.pie.common;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		ArrayList<String> breadCrumbs = new ArrayList<String>();
		return loadXmlDefinition(filename, breadCrumbs);
	}

	public static Document loadXmlDefinition(String filename, ArrayList<String> breadCrumbs) {
		Document xDoc = loadXmlDocument(filename);

		// Remove remarked elements
		List<Node> aRemarks = new ArrayList<>();
		NodeList nl = selectNodes(xDoc.getDocumentElement(), "//Remark | //Note | //*[@Remark] | //Comment");
		if (nl.getLength() > 0) {
			int iLength = nl.getLength();
			for (int i = 0; i < iLength; i++) {
				aRemarks.add(nl.item(i));
			}

			iLength = aRemarks.size();
			for (int i = 0; i < iLength; i++) {
				aRemarks.get(i).getParentNode().removeChild(aRemarks.get(i));
			}
		}

		nl = selectNodes(xDoc.getDocumentElement(), "//IncludeSharedElement");
		if (nl.getLength() > 0) {
			int length = nl.getLength();
			Map<Integer, Element[]> elementsToInsert = new HashMap<Integer, Element[]>();
			for (int i = 0; i < length; i++) {
				Element ele = (Element) nl.item(i);
				String definitionName = ele.getAttribute("DefinitionName");
				String definitionFilename = definitionName;
				String elementID = ele.getAttribute("SharedElementID");
				if (StringUtilities.isNullOrEmpty(definitionName)) {
					throw new RuntimeException("The IncludeSharedElement is missing a value in the required DefinitionName");
				}
				if (StringUtilities.isNullOrEmpty(elementID)) {
					throw new RuntimeException("The IncludeSharedElement is missing a value in the required SharedElementID");
				}
				if (!definitionFilename.toLowerCase().endsWith(".xml")) {
					definitionFilename += ".xml";
				}
				if (FileUtilities.isInvalidFile(definitionFilename)) {
					File temp = new File(filename);
					String absolutePath = temp.getAbsolutePath();
					String definitionPath = absolutePath.substring(0, absolutePath.lastIndexOf(File.separator) + 1);
					definitionFilename = definitionPath + definitionFilename;
				}
				if (FileUtilities.isInvalidFile(definitionFilename)) {
					throw new RuntimeException(String.format("IncludeSharedElement could not find the %s referenced definition", definitionName));
				}

				String crumb = String.format("%s|%s", definitionName, elementID);
				if (breadCrumbs.indexOf(crumb) > -1) {
					throw new RuntimeException(String.format("Possible circular IncludeSharedElement reference detected. Definition %s SharedElementID %s triggered the error.", definitionName, elementID));
				}
				breadCrumbs.add(crumb);

				Document innerDocument = loadXmlDefinition(definitionFilename, breadCrumbs);
				// Look for the shared element
				Node sharedNode = XmlUtilities.selectSingleNode(innerDocument, String.format("//SharedElement[@ID='%s']", elementID));
				if (sharedNode == null) {
					throw new RuntimeException(String.format("IncludeSharedElement could not find a SharedElement with ID=%s referenced in %s", elementID, definitionName));
				}
				NodeList sharedSteps = XmlUtilities.selectNodes(sharedNode, "*");
				int sharedStepsLength = sharedSteps.getLength();
				if (sharedStepsLength > 0) {
					Integer pos = elementsToInsert.size();
					for (int x = 0; x < sharedStepsLength; x++) {
						Element[] elementUpdate = new Element[2];
						elementUpdate[0] = ele;
						elementUpdate[1] = (Element) (sharedSteps.item(x));
						elementsToInsert.put(pos, elementUpdate);
					}
				}
			}
			if (elementsToInsert.size() > 0) {
				Element docElement = xDoc.getDocumentElement();
				int insertsToDo = elementsToInsert.size();
				for (int i = 0; i < insertsToDo; i++) {
					Element parent = elementsToInsert.get(i)[0];
					Element newElement = elementsToInsert.get(i)[1];
					docElement.insertBefore(xDoc.adoptNode(newElement.cloneNode(true)), parent);
				}
			}
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
