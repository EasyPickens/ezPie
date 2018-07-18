package com.fanniemae.ezpie.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

		// NodeList columns = XmlUtilities.selectNodes(dataSource, "Column");
		// if ((columns == null) || (columns.getLength() == 0)) {
		// throw new PieException("Missing required Column elements.");
		// }
		//
		// _columnCount = columns.getLength();
		// _columnNames = new String[_columnCount];
		// _columnPaths = new String[_columnCount][];
		// _columnTypes = new String[_columnCount];
		//
		// for (int i = 0; i < _columnCount; i++) {
		// _columnNames[i] = _session.getAttribute(columns.item(i), "Name");
		// _columnTypes[i] = _session.optionalAttribute(columns.item(i), "DataType", null);
		// String path = _session.getAttribute(columns.item(i), "JsonPath");
		// _columnPaths[1] = path.split("\\.");
		// }
	}

	public Map<String, String> getSchema() {
		return _jsonSchema;
	}

	public List<Map<String, Object>> getData(String jsonString) {
		if (jsonString == null) {
			return new ArrayList<Map<String, Object>>();
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
			List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
			Map<String,Object> starterRow = new HashMap<String,Object>();
			rows.add(starterRow);
			rows = toRows("", json.getJSONObject(i), rows);

			for (int x = 0; x < rows.size(); x++) {
				_rawData.add(rows.get(x));
			}

			// Map<String, Object> row = new HashMap<String, Object>();
			// row = toMapDeep("", json.getJSONObject(i), row);

			// _rawData.add(row);

			// for (int x = 0; x < 100; x++) {
			// _foundItem = false;
			// Map<String, Object> row = new HashMap<String, Object>();
			// // pull index # for each array found.
			// row = toMap("", json.getJSONObject(i), row, x);
			// // Add row to data block
			// if (_foundItem) {
			// _rawData.add(row);
			// } else {
			// break;
			// }
			// }
		}

		// Now that all the data has been read, standardize each row into table form.

		return _rawData;
	}

	protected Set<String> _uniquekeys = new HashSet<String>();
	protected Map<String, String> _jsonSchema = new HashMap<String, String>();
	protected List<Map<String, Object>> _rawData = new ArrayList<Map<String, Object>>();
	protected boolean _foundItem = false;

	protected Map<String, Object> toMap(String prefix, JSONObject json, Map<String, Object> rowData, int arrayIndex) {
		Iterator<String> keys = json.keys();

		while (keys.hasNext()) {
			String key = keys.next();
			String fullkey = "".equals(prefix) ? key : prefix + key;
			Object value = json.get(key);

			if (value == null) {
				continue;
			} else if (value instanceof JSONArray) {
				if (arrayIndex < 0) {
					continue;
				}
				JSONArray jsonArray = (JSONArray) value;
				int length = jsonArray.length();
				if (length > arrayIndex) {
					_foundItem = true;
					Object item = jsonArray.get(arrayIndex);
					rowData = toMap(fullkey + "_", (JSONObject) item, rowData, arrayIndex);
				}
			} else if (value instanceof JSONObject) {
				rowData = toMap(fullkey + "_", (JSONObject) value, rowData, arrayIndex);
			} else {
				_uniquekeys.add(fullkey);

				String valueType = value == null ? "" : value.getClass().getName();
				if (!"".equals(valueType) || !_jsonSchema.containsKey(fullkey)) {
					_jsonSchema.put(fullkey, valueType);
				}
				rowData.put(fullkey, value);
			}
		}

		return rowData;
	}

	// Needs to return a List<Map<String,Object>> containing all the rows of data.
	// One JSON Object could return multiple rows of data when arrays are pivoted.
	protected Map<String, Object> toMapDeep(String prefix, JSONObject json, Map<String, Object> rowData) {
		Iterator<String> keys = json.keys();

		// Pull all the keys from this level of the JSON.
		while (keys.hasNext()) {
			String key = keys.next();
			String fullkey = "".equals(prefix) ? key : prefix + key;
			Object value = json.get(key);

			if (value == null) {
				continue;
			} else if (value instanceof JSONArray) {
				JSONArray jsonArray = (JSONArray) value;
				int length = jsonArray.length();
				if (length == 0) {
					continue;
				}
				rowData = toMapDeep(fullkey + "_", (JSONObject) jsonArray.getJSONObject(0), rowData);
			} else if (value instanceof JSONObject) {
				rowData = toMapDeep(fullkey + "_", (JSONObject) value, rowData);
			} else {
				_uniquekeys.add(fullkey);

				String valueType = value == null ? "" : value.getClass().getName();
				if (!"".equals(valueType) || !_jsonSchema.containsKey(fullkey)) {
					_jsonSchema.put(fullkey, valueType);
				}
				rowData.put(fullkey, value);
			}
		}

		// // Now process the JSONArrays and pull only that information - recursive call into toMapDeep.
		// keys = json.keys();
		// while (keys.hasNext()) {
		// String key = keys.next();
		// String fullkey = "".equals(prefix) ? key : prefix + key;
		// Object value = json.get(key);
		//
		// if (value == null) {
		// continue;
		// } else if (value instanceof JSONArray) {
		// JSONArray jsonArray = (JSONArray) value;
		// int length = jsonArray.length();
		// for (int i=0;i<length;i++) {
		// Object item = jsonArray.get(i);
		// rowData = toMapDeep(fullkey + "_", (JSONObject) item, rowData);
		// }
		// } else {
		// continue;
		// }
		// }

		return rowData;
	}

	// protected List<Map<String,Object>> readJson(String prefix, JSONObject json, Map<String,Object> row) {
	// Iterator<String> keys = json.keys();
	//
	// // Pull all the keys from this level of the JSON.
	// while (keys.hasNext()) {
	// String key = keys.next();
	// String fullkey = "".equals(prefix) ? key : prefix + key;
	// Object value = json.get(key);
	//
	// if (value == null) {
	// continue;
	// } else if (value instanceof JSONArray) {
	// JSONArray jsonArray = (JSONArray) value;
	// int length = jsonArray.length();
	// if (length == 0) {
	// continue;
	// }
	//
	// List<Map<String,Object>> arrayRows = new ArrayList<Map<String,Object>>();
	// for (int i=0;i<length;i++) {
	// Map<String,Object> dataRow = new HashMap<String,Object>();
	// List<Map<String,Object>> newRows = readJson(fullkey+"_", (JSONObject)jsonArray.getJSONObject(i),dataRow);
	// int newCount = newRows.size();
	// if (newCount > 0) {
	// for (int x=0;x<newCount;x++){
	//
	// }
	// }
	// }
	//
	//
	// row = toMapDeep(fullkey + "_", (JSONObject) jsonArray.getJSONObject(0), row);
	// } else if (value instanceof JSONObject) {
	// row = toMapDeep(fullkey + "_", (JSONObject) value, row);
	// } else {
	// _uniquekeys.add(fullkey);
	//
	// String valueType = value == null ? "" : value.getClass().getName();
	// if (!"".equals(valueType) || !_jsonSchema.containsKey(fullkey)) {
	// _jsonSchema.put(fullkey, valueType);
	// }
	// row.put(fullkey, value);
	// }
	// }
	// }

	// Seems to work with a variety of JSON objects, does not work with JSONArray of values.  e.g. "key": [5,3,2,1,3,2]
	protected List<Map<String, Object>> toRows(String prefix, JSONObject json, List<Map<String, Object>> rows) {
		Iterator<String> keys = json.keys();

		// Pull all the keys from this level of the JSON.
		while (keys.hasNext()) {
			String key = keys.next();
			String fullkey = "".equals(prefix) ? key : prefix + key;
			Object value = json.get(key);

			if (value == null) {
				continue;
			} else if (value instanceof JSONArray) {
				JSONArray jsonArray = (JSONArray) value;
				int length = jsonArray.length();
				if (length == 0) {
					continue;
				}

				List<Map<String, Object>> modifiedRows = new ArrayList<Map<String, Object>>();
				for (int i = 0; i < length; i++) {
					List<Map<String, Object>> arrayRows = new ArrayList<Map<String, Object>>();
					Map<String,Object> arrayStarterRow = new HashMap<String,Object>();
					arrayRows.add(arrayStarterRow);
					arrayRows = toRows(fullkey + "_", (JSONObject) jsonArray.getJSONObject(i), arrayRows);
					int arrayLength = arrayRows.size();
					if (arrayLength > 0) {
						for (int x = 0; x < arrayLength; x++) {
							int rowCount = rows.size();
							Map<String, Object> currentRow = arrayRows.get(x);
							for (int y = 0; y < rowCount; y++) {
								Map<String, Object> parentRow = rows.get(y);
								for (Entry<String, Object> kvp : parentRow.entrySet()) {
									currentRow.put(kvp.getKey(), kvp.getValue());
								}
								modifiedRows.add(currentRow);
							}
						}
					}
				}
				rows = modifiedRows;

				// row = toMapDeep(fullkey + "_", (JSONObject) jsonArray.getJSONObject(0), row);
			} else if (value instanceof JSONObject) {
				rows = toRows(fullkey + "_", (JSONObject) value, rows);
			} else {
				_uniquekeys.add(fullkey);

				String valueType = value == null ? "" : value.getClass().getName();
				if (!"".equals(valueType) || !_jsonSchema.containsKey(fullkey)) {
					_jsonSchema.put(fullkey, valueType);
				}
				for (int i = 0; i < rows.size(); i++) {
					rows.get(i).put(fullkey, value);
				}
			}
		}
		return rows;
	}

}
