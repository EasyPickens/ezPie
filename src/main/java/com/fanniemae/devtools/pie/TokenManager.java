package com.fanniemae.devtools.pie;

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

	protected HashMap<String, HashMap<String, String>> _tokens = new HashMap<String, HashMap<String, String>>();
	protected LogManager _logger;

	public TokenManager(Element eleSettings, LogManager logger) {
		_logger = logger;
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
			case "Git":
				loadTokenValues("Git",nl.item(i));
				break;
			}
		}
	}

	public void addToken(String tokenType, String key, String value) {
		HashMap<String, String> aTokenValues = new HashMap<String, String>();
		if (_tokens.containsKey(tokenType))
			aTokenValues = _tokens.get(tokenType);

		aTokenValues.put(key, value);
		_tokens.put(tokenType, aTokenValues);
		if (key.toLowerCase().equals("password")) return;
		String sLogMessage = String.format("%s token value added.\n%s", tokenType, key+"="+value);
		_logger.addMessage("", "@" + tokenType, sLogMessage);
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

			if (_tokens.containsKey(sGroup) && _tokens.get(sGroup).containsKey(sKey)) {
				sValue = sValue.replace(sFullToken, _tokens.get(sGroup).get(sKey));
			} else {
				// if the token is not found, it evaulates to empty string.
				sValue = sValue.replace(sFullToken, "");
			}
		}
		return sValue;
	}

	protected void loadTokenValues(String sTokenType, Node xNode) {
		HashMap<String, String> aKeyValues;
		if (_tokens.containsKey(sTokenType)) {
			aKeyValues = _tokens.get(sTokenType);
		} else {
			aKeyValues = new HashMap<String, String>();
		}
		
		int startCount = aKeyValues.size();

		StringBuilder sb = new StringBuilder();
		NamedNodeMap aAttributes = xNode.getAttributes();
		
		int iUpdateCount = 0;
		int iLen = aAttributes.getLength();
		for (int i = 0; i < iLen; i++) {
			Node xA = aAttributes.item(i);
			String sName = xA.getNodeName();
			String sValue = xA.getNodeValue();
			if (sName.equals("ID")) continue;
			iUpdateCount++;
			aKeyValues.put(sName, sValue);
			if(sName.toLowerCase().equals("password")) continue;
			sb.append(String.format("%s = %s \n", sName, sValue));
		}
		_tokens.put(sTokenType, aKeyValues);
		
		String sLogMessage = "Adding tokens to log manager.";
		if (startCount == 0) {
			sLogMessage = String.format("%,d %s token(s) defined.\n%s", aKeyValues.size(), sTokenType, sb.toString());
		} else if (startCount == aKeyValues.size()) {
			sLogMessage = String.format("%,d %s token value(s) updated.\n%s", iUpdateCount, sTokenType, sb.toString());
		} else if (startCount < aKeyValues.size()) {
			sLogMessage = String.format("%,d %s token value(s) added/updated.\n%s", aKeyValues.size()-startCount, sTokenType, sb.toString());
		}
		_logger.addMessage("", "@" + sTokenType, sLogMessage);
	}
}
