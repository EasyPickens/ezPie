package com.fanniemae.automation;

import java.util.HashMap;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.w3c.dom.Node.ELEMENT_NODE;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-14
 * 
 */
public class TokenManager {

	protected HashMap<String, HashMap<String, String>> _aTokens = new HashMap<String, HashMap<String, String>>();
	protected LogManager _Log;

	public TokenManager(Element eleSettings, LogManager oLogger) {
		_Log = oLogger;
		NodeList nl = eleSettings.getChildNodes();
		int iLength = nl.getLength();
		for (int i = 0; i < iLength; i++) {
			if (nl.item(i).getNodeType() != ELEMENT_NODE)
				continue;

			switch (nl.item(i).getNodeName()) {
			case "Configuration":
				loadTokenValues("Configuration", nl.item(i));
				break;
			case "Constants":
				loadTokenValues("Constants", nl.item(i));
				break;
			}
		}
	}

	public void addToken(String tokenType, String key, String value) {
		HashMap<String, String> aTokenValues = new HashMap<String, String>();
		if (_aTokens.containsKey(tokenType))
			aTokenValues = _aTokens.get(tokenType);

		aTokenValues.put(key, value);
		_aTokens.put(tokenType, aTokenValues);
	}

	public void addTokens(String tokenType, Node nodeTokenValues) {
		loadTokenValues(tokenType, nodeTokenValues);
	}

	public String getAttribute(Element ele, String sName) {
		return resolveTokens(ele.getAttribute(sName));
	}

	public String resolveTokens(String sValue) {
		return resolveTokens(sValue, null);
	}

	public String resolveTokens(String sValue, Object[] aDataRow) {
		if ((sValue.indexOf("@") == -1) || (sValue.indexOf("~") == -1))
			return sValue;

		int iTokenSplit = 0;
		int iTokenEnd = 0;
		String[] aTokens = sValue.split("@");

		for (int i = 0; i < aTokens.length; i++) {
			iTokenSplit = aTokens[i].indexOf('.');
			iTokenEnd = aTokens[i].indexOf('~');
			if ((iTokenSplit == -1) || (iTokenEnd == -1))
				continue;

			String sFullToken = "@" + aTokens[i].substring(0, iTokenEnd + 1);
			String sGroup = aTokens[i].substring(0, iTokenSplit);
			String sKey = aTokens[i].substring(iTokenSplit + 1, iTokenEnd);

			// Skip data tokens if no row of data is provided.
			if ((aDataRow == null) && sGroup.equals("Data"))
				continue;

			if (_aTokens.containsKey(sGroup) && _aTokens.get(sGroup).containsKey(sKey)) {
				sValue = sValue.replace(sFullToken, _aTokens.get(sGroup).get(sKey));
			}
		}
		return sValue;
	}

	protected void loadTokenValues(String sTokenType, Node xNode) {
		HashMap<String, String> aKeyValues = new HashMap<String, String>();
		if (_aTokens.containsKey(sTokenType))
			aKeyValues = _aTokens.get(sTokenType);

		StringBuilder sb = new StringBuilder();
		NamedNodeMap aAttributes = xNode.getAttributes();
		int iLen = aAttributes.getLength();
		for (int i = 0; i < iLen; i++) {
			Node xA = aAttributes.item(i);
			String sName = xA.getNodeName();
			String sValue = xA.getNodeValue();
			if (sName.equals("ID")) continue;
			aKeyValues.put(sName, sValue);
			sb.append(String.format("%s = %s \n", sName, sValue));
		}
		_aTokens.put(sTokenType, aKeyValues);
		_Log.addMessage("", "@" + sTokenType, String.format("%s\n%,d tokens defined.", sb.toString(), aKeyValues.size()));
	}
}
