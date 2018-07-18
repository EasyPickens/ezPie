/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.data.connectors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.DataUtilities;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.common.RestConverter;
import com.fanniemae.ezpie.common.RestUtilities;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.common.XmlUtilities;
import com.fanniemae.ezpie.data.utilities.TreeNode;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 *
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @author Tara Tritt
 * @since 2016-04-29
 * 
 */

public class RestConnector extends DataConnector {
	protected String _validateCertificate = "true";
	protected Element _conn;
	protected String _url;
	protected String _username;
	protected String _password;
	protected String _proxyHost;
	protected int _proxyPort;
	protected String _proxyUsername;
	protected String _proxyPassword;
	protected NodeList _columns;

	protected Object[][] _rows;
	protected int _index = 0;
	protected DataType[] _dataTypes;

	protected List<String> _columnKeys;
	protected List<Map<String, Object>> _data;

	protected int _rowCount;

	protected Map<String, String> _requestHeader = new HashMap<String, String>();

	public RestConnector(SessionManager session, Element dataSource, Boolean isSchemaOnly) {
		super(session, dataSource, isSchemaOnly);
		_connectionName = _session.getAttribute(dataSource, "ConnectionName");
		_session.addLogMessage("", "ConnectionName", _connectionName);
		_conn = _session.getConnection(_connectionName);
		_username = _session.getAttribute(_conn, "Username");
		_password = _session.getAttribute(_conn, "Password");
		_proxyHost = _session.getAttribute(_conn, "ProxyHost");
		_proxyPort = StringUtilities.toInteger(_session.getAttribute(_conn, "ProxyPort"));
		_proxyUsername = _session.getAttribute(_conn, "ProxyUsername");
		_proxyPassword = _session.getAttribute(_conn, "ProxyPassword");
		_url = _session.requiredAttribute(dataSource, "URL");

		_columns = XmlUtilities.selectNodes(_dataSource, "Column");

		NodeList headerList = XmlUtilities.selectNodes(_conn, "Header");
		if (headerList.getLength() > 0) {
			int length = headerList.getLength();
			for (int i = 0; i < length; i++) {
				Element header = (Element) headerList.item(i);
				_requestHeader.put(_session.requiredAttribute(header, "Key"), _session.optionalAttribute(header, "Value", ""));
			}
		}
	}

	@Override
	public Boolean open() {
		try {
			// String response = RestUtilities.sendGetRequest(_url, _proxyHost, _proxyPort, _proxyUsername, _proxyPassword, _username, _password, _requestHeader);
			String response = FileUtilities.loadFile("C:\\Developers\\Code\\TestDirectory\\_Resources\\UrbanCodeRelease.json");
			int length = (response == null) ? 0 : response.length();
			_session.addLogMessage("", "RestConnector", String.format("View Raw Response (%,d bytes)", length), "file://" + FileUtilities.writeRandomTextFile(_session.getLogPath(), response));

			RestConverter rc = new RestConverter(_session, _dataSource);
			_data = rc.getData(response);
			Map<String, String> fullSchema = rc.getSchema();

			_columnKeys = new ArrayList<String>();
			NodeList columns = XmlUtilities.selectNodes(_dataSource, "Column");
			if (columns.getLength() > 0) {
				_dataSchema = new String[columns.getLength()][2];
				int col = columns.getLength();
				for (int i = 0; i < col; i++) {
					String columnName = _session.getAttribute(_columns.item(i), "Name");
					String path = _session.getAttribute(_columns.item(i), "JsonPath");
					String key = path.replace('.', '_');
					if (fullSchema.containsKey(path.replace('.', '_'))) {
						_dataSchema[i][0] = columnName;
						_dataSchema[i][1] = fullSchema.get(key);
						_columnKeys.add(key);
					} else {
						_dataSchema[i][0] = columnName;
						_dataSchema[i][1] = "java.lang.String";
						_columnKeys.add(key);
					}
				}
			} else {
				_dataSchema = new String[fullSchema.size()][2];

				int i = 0;
				for (Map.Entry<String, String> kvp : fullSchema.entrySet()) {
					_dataSchema[i][0] = kvp.getKey();
					_dataSchema[i][1] = kvp.getValue();
					_columnKeys.add(kvp.getKey());
					i++;
				}
			}

			_rowCount = _data.size();

			// int numColumns = _columns.getLength();
			// _session.addLogMessage("", "RestConnector", String.format("%,d columns found", numColumns));
			//
			// // Read/create column names.
			// _dataSchema = new String[numColumns][2];
			// _dataTypes = new DataType[numColumns];
			//
			// // get columns from definition file and split
			// ArrayList<TreeNode<String>> columns = new ArrayList<TreeNode<String>>();
			// ArrayList<String[]> tempPaths = new ArrayList<String[]>();
			// for (int i = 0; i < numColumns; i++) {
			// String columnName = _session.getAttribute(_columns.item(i), "Name");
			// String path = _session.getAttribute(_columns.item(i), "JsonPath");
			// String columnType = _session.optionalAttribute(_columns.item(i), "DataType", null);
			// String[] pathParts = path.split("\\.");
			// tempPaths.add(pathParts);
			// _dataSchema[i][0] = columnName;
			// _dataSchema[i][1] = columnType;
			// }
			//
			// // build tree that will contain all of the JSON attributes to return
			// for (int m = 0; m < tempPaths.size(); m++) {
			// boolean found = false;
			// for (int n = 0; n < columns.size(); n++) {
			// if (searchTree(columns.get(n), tempPaths.get(m), 0)) {
			// found = true;
			// }
			// }
			// if (!found) {
			// columns.add(new TreeNode<String>(tempPaths.get(m)[0]));
			// searchTree(columns.get(columns.size() - 1), tempPaths.get(m), 0);
			// }
			// }
			//
			// TreeNode<String> root = new TreeNode<String>("root");
			// for (int n = 0; n < columns.size(); n++) {
			// root.addChild(columns.get(n));
			// }
			//
			// Object json = new JSONTokener(response).nextValue();
			//
			// // create rows by parsing JSON and using the tree with the attributes to search for
			// ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
			// if (json instanceof JSONObject) {
			// rows = buildRows((JSONObject) json, rows, root);
			// } else if (json instanceof JSONArray) {
			// ArrayList<ArrayList<String>> parentRows = new ArrayList<ArrayList<String>>(rows);
			// for (int i = 0; i < ((JSONArray) json).length(); i++) {
			// rows.addAll(buildRows(((JSONArray) json).getJSONObject(i), parentRows, root));
			// }
			// }
			//
			// // determine data types and cast values to correct type
			// dataTypeRows(rows);

		} catch (JSONException ex) {
			throw new RuntimeException("Error while trying to make REST request: " + ex.getMessage(), ex);
		}
		return true;
	}

	@Override
	public Boolean eof() {
		if ((_rowLimit != -1) && (_index >= _rowLimit)) {
			return true;
		} else if (_index < _rowCount) {
			return false;
		}
		return true;
	}

	@Override
	public Object[] getDataRow() {
		Map<String, Object> dataRow = _data.get(_index);

		int length = _dataSchema.length;
		Object[] row = new Object[length];
		for (int i = 0; i < length; i++) {
			row[i] = dataRow.get(_columnKeys.get(i));
		}
		_index++;
		return row;
	}

	@Override
	public void close() {

	}

	private void dataTypeRows(ArrayList<ArrayList<String>> rows) {
		// discover longest row
		int numColumns = 0;
		for (int i = 0; i < rows.size(); i++) {
			ArrayList<String> row = rows.get(i);
			for (int j = 0; j < row.size(); j++) {
				numColumns = numColumns > row.size() ? numColumns : row.size();
			}
		}
		String[] typeStrings = new String[numColumns];
		_dataTypes = new DataType[numColumns];
		_rows = new Object[rows.size()][numColumns];
		for (int i = 0; i < rows.size(); i++) {
			ArrayList<String> columns = rows.get(i);
			for (int j = 0; j < columns.size(); j++) {
				String value = columns.get(j);
				String dataType = StringUtilities.getDataType(value, typeStrings[j]);
				typeStrings[j] = dataType;
			}
		}

		// Update null types with the new information
		for (int i = 0; i < _dataSchema.length; i++) {
			if (_dataSchema[i][1] == null) {
				_dataSchema[i][1] = typeStrings[i];
			}
			_dataTypes[i] = DataUtilities.dataTypeToEnum(_dataSchema[i][1]);
		}

		// populate the data rows
		for (int i = 0; i < rows.size(); i++) {
			ArrayList<String> columns = rows.get(i);
			for (int j = 0; j < columns.size(); j++) {
				String value = columns.get(j);
				_rows[i][j] = castValue(j, value);
			}
		}
	}

	protected Object castValue(int i, String value) {
		if (StringUtilities.isNullOrEmpty(value)) {
			return null;
		}

		switch (_dataTypes[i]) {
		case StringData:
			return value;
		case DateData:
			return StringUtilities.toDate(value);
		case IntegerData:
			return StringUtilities.toInteger(value);
		case LongData:
			return StringUtilities.toLong(value);
		case DoubleData:
			return StringUtilities.toDouble(value);
		case BigDecimalData:
			return StringUtilities.toBigDecimal(value);
		case BooleanData:
			return StringUtilities.toBoolean(value);
		default:
			throw new PieException(String.format("%s string conversion not currently available.", DataType.values()[i]));
		}
	}

	private ArrayList<ArrayList<String>> buildRows(JSONObject json, ArrayList<ArrayList<String>> parentRows, TreeNode<String> column) {
		Object j = null;
		if ("root".equals(column.getData())) {
			j = json;
		} else if (!json.has(column.getData())) {
			j = "";
		} else {
			j = json.get(column.getData());
		}

		if ((j == null) || "null".equalsIgnoreCase(j.toString())) {
			j = "";
		}

		if (j instanceof JSONArray) {
			// It's an array
			ArrayList<ArrayList<String>> oldParentRows = parentRows;
			ArrayList<ArrayList<String>> childRows = new ArrayList<ArrayList<String>>();
			if (((JSONArray) j).length() == 0) {
				return parentRows;
			}
			for (int m = 0; m < ((JSONArray) j).length(); m++) {
				parentRows = oldParentRows;
				JSONObject jj = ((JSONArray) j).getJSONObject(m);
				Iterator<TreeNode<String>> iter = column.iterator();
				while (iter.hasNext()) {
					ArrayList<ArrayList<String>> returned = buildRows(jj, parentRows, iter.next());
					parentRows = returned;
				}
				childRows.addAll(parentRows);
			}

			return childRows;
		} else if (j instanceof JSONObject) {
			// It's an object
			ArrayList<ArrayList<String>> childRows = new ArrayList<ArrayList<String>>();
			Iterator<TreeNode<String>> iter = column.iterator();
			while (iter.hasNext()) {
				ArrayList<ArrayList<String>> returned = buildRows((JSONObject) j, parentRows, iter.next());
				parentRows = returned;
			}
			childRows.addAll(parentRows);

			return childRows;
		} else {
			// It's a string, number etc.
			ArrayList<ArrayList<String>> childRows = new ArrayList<ArrayList<String>>();
			for (int n = 0; n < parentRows.size(); n++) {
				ArrayList<String> temp = new ArrayList<String>(parentRows.get(n));
				temp.add(j.toString());
				childRows.add(temp);
			}
			if (parentRows.size() == 0) {
				ArrayList<String> temp = new ArrayList<String>();
				temp.add(j.toString());
				if (column.getChildLength() > 1) {
					int length = column.getChildLength() - 1;
					for (int i = 0; i < length; i++) {
						temp.add("");
					}
				}
				childRows.add(temp);
			}

			return childRows;
		}
	}

	private boolean searchTree(TreeNode<String> node, String[] pathParts, int index) {
		if (pathParts[index].equals(node.getData())) {
			index++;
			if (pathParts.length <= index) {
				return true;
			}
			boolean found = false;
			Iterator<TreeNode<String>> iter = node.getChildren().iterator();
			while (iter.hasNext()) {
				TreeNode<String> next = iter.next();
				if (searchTree(next, pathParts, index))
					found = true;
			}
			if (!found) {
				node.addChild(pathParts[index]);
				searchTree(node.getChildren().get(node.getChildren().size() - 1), pathParts, index);
			}
			return true;
		} else {
			return false;
		}
	}

}
