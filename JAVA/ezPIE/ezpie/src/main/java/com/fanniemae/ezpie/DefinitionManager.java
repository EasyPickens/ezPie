/**
 *  
 * Copyright (c) 2017 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
**/

package com.fanniemae.ezpie;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.common.Constants;
import com.fanniemae.ezpie.common.Encryption;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.XmlUtilities;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2017-07-31
 * 
 */

public class DefinitionManager {
	protected SessionManager _session = null;

	protected byte[][] _encryptionKey = null;

	protected boolean _isSettingsFile = false;

	protected String _currentDefinitionFilename = "";

	public DefinitionManager() {
	}

	public DefinitionManager(SessionManager session, byte[][] encryptionKey) {
		_session = session;
		_encryptionKey = encryptionKey;
	}

	public Document loadFile(String filename) {
		if (filename == null) {
			return null;
		}

		_isSettingsFile = filename.toLowerCase().contains("_settings.xml");
		ArrayList<String> breadCrumbs = new ArrayList<String>();
		return loadDefinition(filename, null, breadCrumbs);
	}

	private Document encryptSecureElements(Document doc) {
		boolean needToSave = false;
		NodeList nlSecuredAttributes = XmlUtilities.selectNodes(doc.getDocumentElement(), String.format("//@*[contains(name(),'%s')]", Constants.SECURE_SUFFIX));

		if (nlSecuredAttributes != null) {
			int length = nlSecuredAttributes.getLength();
			if ((length > 0) && (_encryptionKey == null)) {
				throw new RuntimeException(String.format("File %s contains secured attribute values, missing encryption key in settings file.", _currentDefinitionFilename));
			}
			for (int i = 0; i < length; i++) {
				String name = nlSecuredAttributes.item(i).getNodeName();
				String value = nlSecuredAttributes.item(i).getNodeValue();
				if (!name.endsWith(Constants.SECURE_SUFFIX) || value.startsWith(Constants.ENCRYPTED_PREFIX)) {
					continue;
				}
				needToSave = true;
				nlSecuredAttributes.item(i).setNodeValue(String.format("%s%s", Constants.ENCRYPTED_PREFIX, Encryption.encryptToString(value, _encryptionKey)));
			}
			if (needToSave) {
				saveXmlFile(doc);
			}
		}
		return doc;
	}

	private boolean exists(String filename) {
		return FileUtilities.isValidFile(filename);
	}

	private Document loadDefinition(String filename, String sharedElementID, ArrayList<String> breadCrumbs) {
		Document doc = XmlUtilities.loadXmlFile(filename);
		if (_isSettingsFile) {
			Node config = XmlUtilities.selectSingleNode(doc, "//Configuration");
			if (config != null) {
				String key = ((Element) config).getAttribute("EncryptionKey");
				if (!key.isEmpty()) {
					_encryptionKey = Encryption.setupKey(key);
				}
			}
		}
		_currentDefinitionFilename = filename;
		encryptSecureElements(doc);
		_currentDefinitionFilename = "";

		XmlUtilities.removeRemarkedElements(doc.getDocumentElement());

		// Import shared element is not supported in the settings file - too prone to configuration errors.
		if (!_isSettingsFile) {
			String xpath = (sharedElementID == null) ? "//ImportSharedElement" : String.format("//SharedElement[@ID='%s']/ImportSharedElement", sharedElementID);
			NodeList nl = XmlUtilities.selectNodes(doc.getDocumentElement(), xpath);

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

					Document innerDocument = loadDefinition(definitionFilename, elementID, loopCrumbs);

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

						parent.getParentNode().insertBefore(doc.adoptNode(newElement.cloneNode(true)), parent);
					}
					// Remove ImportSharedElements
					XmlUtilities.removeElements(doc, "//ImportSharedElement");
				}
			}
		}
		// Remove left over empty text nodes
		XmlUtilities.removeWhitespace(doc);
		return doc;
	}

	private void saveXmlFile(Document doc) {
		try {
			XmlUtilities.saveXmlFile(_currentDefinitionFilename, doc);
		} catch (Exception ex) {
			if (_session != null) {
				File file = new File(_currentDefinitionFilename);
				_session.addLogMessage("** Warning **", "Save File", String.format("Could not save the %s definition with secured attribute values.", file.getName()));
			}
		}
	}
}
