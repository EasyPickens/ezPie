package com.fanniemae.ezpie.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.SessionManager;

public class RestConverter {

	protected SessionManager _session;

	protected int _columnCount;

	protected String[] _columnNames;
	protected String[][] _columnPaths;
	protected String[] _columnTypes;

	protected List<Object[]> _data = new ArrayList<Object[]>();

	public RestConverter(SessionManager session, Element dataSource) {

		_session = session;

		NodeList columns = XmlUtilities.selectNodes(dataSource, "Column");
		if ((columns == null) || (columns.getLength() == 0)) {
			throw new PieException("Missing required Column elements.");
		}

		_columnCount = columns.getLength();
		_columnNames = new String[_columnCount];
		_columnPaths = new String[_columnCount][];
		_columnTypes = new String[_columnCount];

		for (int i = 0; i < _columnCount; i++) {
			_columnNames[i] = _session.getAttribute(columns.item(i), "Name");
			_columnTypes[i] = _session.optionalAttribute(columns.item(i), "DataType", null);
			String path = _session.getAttribute(columns.item(i), "JsonPath");
			_columnPaths[1] = path.split("\\.");
		}
	}

	public List<Object[]> pullData(String jsonString) {
		if (jsonString == null) {
			return new ArrayList<Object[]>();
		}

		JSONArray json;
		if (jsonString.startsWith("[")) {
			json = new JSONArray(jsonString);
		} else if (jsonString.startsWith("{")) {
			json = new JSONArray();
			json.put(new JSONObject(jsonString));
		} else {
			throw new PieException("Returned string does not match JSON specifications.");
		}

		int length = json.length();
		for (int i = 0; i < length; i++) {
			Map<String, Object> row = new HashMap<String, Object>();
			row = toMap("", json.getJSONObject(i), row);
			_rawData.add(row);
		}

		return null;
	}

	protected Set<String> _uniquekeys = new HashSet<String>();
	protected List<Map<String, Object>> _rawData = new ArrayList<Map<String, Object>>();

	protected Map<String, Object> toMap(String prefix, JSONObject json, Map<String, Object> rowData) {
		Iterator<String> keys = json.keys();

		while (keys.hasNext()) {
			String key = keys.next();
			String fullkey = "".equals(prefix) ? key : prefix + key;
			Object value = json.get(key);

			if (value == null) {
				continue;
			} else if (value instanceof JSONArray) {
				// skip for now, will come back at end
				continue;
			} else if (value instanceof JSONObject) {
				rowData = toMap(fullkey + "_", (JSONObject) value, rowData);
			} else {
				_uniquekeys.add(fullkey);
				rowData.put(fullkey, value);
			}
		}

		return rowData;
	}

	// protected Object[] readRows(JSONArray json) {
	// int length = json.length();
	// for (int i = 0; i < length; i++) {
	// Map<String, Object> row = new HashMap<String, Object>();
	// //Object[] row = new Object[_columnCount];
	//
	// // Read the array and loop through any keys or objects.
	// Object jsonPiece = json.get(i);
	// if (jsonPiece instanceof JSONArray) {
	//
	// } else if (jsonPiece instanceof JSONObject) {
	//
	// } else {
	// // This is a kvp
	//
	// }
	// }
	// return null;
	// }
}
