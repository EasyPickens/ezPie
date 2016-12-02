package com.fanniemae.devtools.pie;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fanniemae.devtools.pie.common.StringUtilities;
import com.fanniemae.devtools.pie.common.XmlUtilities;

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
	protected Date _startDateTime = new Date();

	protected enum LogVisibility {
		NONE, TOKEN_NAME, FULL
	};

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
			case "Tokens":
				loadTokenValues(nl.item(i));
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
		if (hideIt(key)) {
			_logger.addMessage("", "Token Added", String.format("@%s.%s~ = *****", tokenType, key, value));
			return;
		}
		_logger.addMessage("", "Token Added", String.format("@%s.%s~ = %s", tokenType, key, value));
	}

	public void addTokens(Node tokenNode) {
		loadTokenValues(tokenNode);
	}

	public void addTokens(String tokenType, Node nodeTokenValues) {
		loadTokenValues(tokenType, nodeTokenValues);
	}

	public void addTokens(String tokenType, String[][] kvps) {
		loadTokenValues(tokenType, kvps);
	}

	public String getAttribute(Element ele, String sName) {
		return resolveTokens(ele.getAttribute(sName));
	}

	public String getTokenValue(String tokenType, String tokenKey) {
		if (_tokens.containsKey(tokenType) && _tokens.get(tokenType).containsKey(tokenKey)) {
			return _tokens.get(tokenType).get(tokenKey);
		}
		return "";
	}

	public String resolveTokens(String sValue) {
		return resolveTokens(sValue, null);
	}

	public String resolveTokens(String value, Object[] dataRow) {
		if (value == null)
			return value;
		
		String rawString = (dataRow == null) ? value.replace("@Data.","|Data|") : value;
		
		int tokenStart = rawString.indexOf("@");
		if (tokenStart == -1)
			return value;
		
		int tokenMid = rawString.indexOf(".", tokenStart);
		if (tokenMid == -1)
			return value;
		
		int tokenEnd = rawString.indexOf("~", tokenMid);
		if (tokenEnd == -1)
			return value;

		int iTokenSplit = 0;
		int iTokenEnd = 0;
		String[] aTokens = value.split("@");

		for (int i = 0; i < aTokens.length; i++) {
			iTokenSplit = aTokens[i].indexOf('.');
			iTokenEnd = aTokens[i].indexOf('~');
			if ((iTokenSplit == -1) || (iTokenEnd == -1))
				continue;

			String sFullToken = "@" + aTokens[i].substring(0, iTokenEnd + 1);
			String sGroup = aTokens[i].substring(0, iTokenSplit);
			String sKey = aTokens[i].substring(iTokenSplit + 1, iTokenEnd);

			// Skip data tokens if no row of data is provided.
			if ((dataRow == null) && sGroup.equals("Data"))
				continue;

			// System tokens call methods
			if ("System".equals(sGroup)) {
				switch (sKey) {
				case "CurrentDateTimeString":
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
					value = value.replace(sFullToken, sdf.format(new Date()));
					break;
				case "StartDateTimeString":
					SimpleDateFormat sdfStart = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
					value = value.replace(sFullToken, sdfStart.format(_startDateTime));
					break;
				case "ISOStartDateTime":
					SimpleDateFormat sdfISO = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					value = value.replace(sFullToken, sdfISO.format(_startDateTime));
					break;
				case "ElapsedTime":  // returns minutes.
					Date dtCurrent = new Date();
					long minutes = (dtCurrent.getTime() - _startDateTime.getTime())/60000;
					value = value.replace(sFullToken, String.format("%d", minutes));
					break;					
				case "UUID":
					value = value.replace(sFullToken, UUID.randomUUID().toString());
					break;
				}
			} else if (_tokens.containsKey(sGroup) && _tokens.get(sGroup).containsKey(sKey)) {
				value = value.replace(sFullToken, _tokens.get(sGroup).get(sKey));
			} else {
				// if the token is not found, it evaluates to empty string.
				value = value.replace(sFullToken, "");
			}
		}
		return resolveTokens(value, dataRow);
	}

	protected void loadTokenValues(String tokenType, String[][] kvps) {
		HashMap<String, String> tokenKeyValues;
		if (_tokens.containsKey(tokenType)) {
			tokenKeyValues = _tokens.get(tokenType);
		} else {
			tokenKeyValues = new HashMap<String, String>();
		}

		StringBuilder sb = new StringBuilder();
		int length = kvps.length;
		for (int i = 0; i < length; i++) {
			if (i > 0)
				sb.append("\n");
			String name = kvps[i][0];
			String value = kvps[i][1];
			tokenKeyValues.put(name, value);
			if (hideIt(name)) {
				sb.append(String.format("@%s.%s~", tokenType, name, value));
			} else {
				sb.append(String.format("@%s.%s~ = %s", tokenType, name, value));
			}
		}
		_tokens.put(tokenType, tokenKeyValues);
		_logger.addMessage("", length == 1 ? "Token Added" : "Tokens Added", sb.toString());

	}

	protected void loadTokenValues(Node tokenNode) {
		if (tokenNode == null)
			return;

		NodeList nl = XmlUtilities.selectNodes(tokenNode, "*");
		int nodeCount = nl.getLength();
		if (nodeCount == 0)
			return;

		LogVisibility defaultVisibility = HideStatus(((Element) tokenNode).getAttribute("Hide"), LogVisibility.FULL);
		int linesAdded = 0;
		int tokensAdded = 0;
		Boolean addNewLine = false;
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < nodeCount; i++) {
			String tokenType = nl.item(i).getNodeName();
			if ("|configuration|system|environment|application|data".indexOf(tokenType.toLowerCase()) != -1) {
				throw new RuntimeException(String.format("%s is one of the reserved token types.  Please rename your token type.",tokenType));
			}
			LogVisibility showLevel = HideStatus(((Element) nl.item(i)).getAttribute("Hide"), defaultVisibility);
			NamedNodeMap attributes = nl.item(i).getAttributes();
			int attrCount = attributes.getLength();
			if (attrCount == 0)
				continue;

			HashMap<String, String> tokenKeyValues = _tokens.containsKey(tokenType) ? _tokens.get(tokenType) : new HashMap<String, String>();
			for (int x = 0; x < attrCount; x++) {
				Node xA = attributes.item(x);
				String name = xA.getNodeName();
				String value = xA.getNodeValue();

				if ("Hide".equals(name))
					continue;

				tokenKeyValues.put(name, value);
				tokensAdded++;
				if (showLevel == LogVisibility.NONE)
					continue;

				if (addNewLine)
					sb.append("\n");

				if (hideIt(name) || (showLevel == LogVisibility.TOKEN_NAME)) {
					sb.append(String.format("@%s.%s~", tokenType, name));
				} else {
					sb.append(String.format("@%s.%s~ = %s", tokenType, name, value));
				}
				linesAdded++;
				addNewLine = true;
			}
			_tokens.put(tokenType, tokenKeyValues);
		}
		if ((tokensAdded > 0) && (linesAdded == 0)) {
			_logger.addMessage("", tokensAdded == 1 ? "Token Added" : "Tokens Added", String.format("%,d tokens added", tokensAdded));
		} else {
			_logger.addMessage("", linesAdded == 1 ? "Token Added" : "Tokens Added", sb.toString());
		}
	}

	protected void loadTokenValues(String tokenType, Node node) {
		HashMap<String, String> tokenKeyValues;
		if (_tokens.containsKey(tokenType)) {
			tokenKeyValues = _tokens.get(tokenType);
		} else {
			tokenKeyValues = new HashMap<String, String>();
		}

		LogVisibility visibility = HideStatus(((Element) node).getAttribute("Hide"), LogVisibility.FULL);
		NamedNodeMap attributes = node.getAttributes();

		int linesAdded = 0;
		int tokensAdded = 0;
		int length = attributes.getLength();
		Boolean addNewLine = false;
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < length; i++) {
			Node xA = attributes.item(i);
			String name = xA.getNodeName();
			String value = xA.getNodeValue();

			if ("Hide".equalsIgnoreCase(name))
				continue;

			tokenKeyValues.put(name, value);
			tokensAdded++;
			if (visibility == LogVisibility.NONE)
				continue;

			if (addNewLine)
				sb.append("\n");
			if (hideIt(name) && (visibility == LogVisibility.TOKEN_NAME)) {
				sb.append(String.format("@%s.%s~", tokenType, name));
			} else {
				sb.append(String.format("@%s.%s~ = %s", tokenType, name, value));
			}
			linesAdded++;
			addNewLine = true;
		}
		_tokens.put(tokenType, tokenKeyValues);
		if (linesAdded == 0) {
			_logger.addMessage("", linesAdded == 1 ? "Token Added" : "Tokens Added", String.format("%,d tokens added", tokensAdded));
		} else {
			_logger.addMessage("", linesAdded == 1 ? "Token Added" : "Tokens Added", sb.toString());
		}
	}

	protected boolean hideIt(String value) {
		if (value == null)
			return false;

		value = value.toLowerCase();
		if (value.startsWith("password") || value.endsWith("password") || value.contains("password"))
			return true;
		else if (value.startsWith("user") || value.endsWith("user") || value.contains("user"))
			return true;
		return false;
	}

	protected LogVisibility HideStatus(String value, LogVisibility defaultVisibility) {
		if (StringUtilities.isNullOrEmpty(value))
			return defaultVisibility;

		if ("value".equalsIgnoreCase(value) || "values".equalsIgnoreCase(value))
			return LogVisibility.TOKEN_NAME;
		else if ("token".equalsIgnoreCase(value) || "tokens".equalsIgnoreCase(value))
			return LogVisibility.NONE;

		return defaultVisibility;
	}
}
