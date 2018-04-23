package com.fanniemae.ezpie.charts;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.ArrayUtilities;
import com.fanniemae.ezpie.common.DateUtilities;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.common.XmlUtilities;
import com.fanniemae.ezpie.datafiles.DataReader;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

public class Chart {
	
	protected SessionManager _session;
	protected Element _action;
	protected String _actionName;

	protected Map<String, List<Object>> _dataLayouts = new HashMap<String, List<Object>>();
	protected Map<String, int[]> _dataColumnIndexes = new HashMap<String, int[]>();

	protected String[] _columnNames;
	protected DataType[] _dataTypes;
	protected JSONArray _fullData = new JSONArray();

	public Chart(SessionManager session, Element action) {
		//super(session, action, true);
		_session = session;
		_action = (Element)action;
		_actionName = _session.getAttribute(_action, "Name");
	}

	public JSONObject buildChartJson(HashMap<String, String> dataTokens) {
		_session.setDataTokens(dataTokens);
		String dataSetName = _session.requiredAttribute(_action, "DataSetName");
		try (DataReader dr = new DataReader(_session.getDataStream(dataSetName))) {
			_columnNames = dr.getColumnNames();
			_dataTypes = dr.getDataTypes();

			// Setup the index arrays and initialize the data layout hash maps for all the metric columns
			NodeList dataLayouts = XmlUtilities.selectNodes(_action, "DataLayout");
			if ((dataLayouts == null) || (dataLayouts.getLength() < 1)) {
				throw new PieException(String.format("%s element requires at least one child DataLayout element.", _actionName));
			}
			int length = dataLayouts.getLength();
			String[] dataNames = new String[length];
			for (int i = 0; i < length; i++) {
				String name = _session.requiredAttribute(dataLayouts.item(i), "Name");
				String[] columnNames = StringUtilities.split(_session.requiredAttribute(dataLayouts.item(i), "DataRow"));
				int[] columnIndexes = null;
				if ((columnNames != null) && (columnNames.length > 0)) {
					columnIndexes = new int[columnNames.length];
					for (int x = 0; x < columnNames.length; x++) {
						columnIndexes[x] = ArrayUtilities.indexOf(_columnNames, columnNames[x]);
					}
				}
				_dataColumnIndexes.put(name, columnIndexes);
				_dataLayouts.put(name, new ArrayList<Object>());
				dataNames[i] = name;
			}

			// Populate the data arrays with the metric values
			while (!dr.eof()) {
				Object[] dataRow = dr.getDataRow();
				for (int i = 0; i < dataNames.length; i++) {
					int[] columnIndexes = _dataColumnIndexes.get(dataNames[i]);
					if (columnIndexes.length == 1) {
						_dataLayouts.get(dataNames[i]).add(getMetric(_dataTypes[columnIndexes[0]], dataRow[columnIndexes[0]]));
					} else {
						Object[] rowValues = new Object[columnIndexes.length];
						for (int col = 0; col < columnIndexes.length; col++) {
							rowValues[col] = getMetric(_dataTypes[columnIndexes[col]], dataRow[columnIndexes[col]]);
						}
						_dataLayouts.get(dataNames[i]).add(rowValues);
					}
				}
				
				// build a full copy of the dataset to populate a supporting table if needed.
				JSONObject jsonDataRow = new JSONObject();
				for (int i = 0; i < dataRow.length; i++) {
					jsonDataRow.put(_columnNames[i], getMetric(_dataTypes[i], dataRow[i]));
				}
				_fullData.put(jsonDataRow);
			}
			dr.close();
		} catch (Exception ex) {
			throw new PieException(String.format("Error while converting %s datastream into chart data. %s", dataSetName, ex.getMessage()), ex);
		}
		_session.clearDataTokens();

		// Assemble the final JSON data and write a file.
		JSONObject chartJson = new JSONObject();
		JSONObject chartSettings = convertToJson();
		chartJson.put("Name", _session.requiredAttribute(_action, "Name"));
		chartJson.put("Chart", chartSettings);
		chartJson.put("Data",_fullData);
		chartJson.put("ColumnNames",_columnNames);
		return chartJson;
	}

	protected Object getMetric(DataType dataType, Object value) {
		if (value == null) {
			return null;
		} else if (dataType == DataType.DateData) {
			return DateUtilities.toIsoString((Date) value);
		} else {
			return value;
		}
	}

	protected JSONObject convertToJson() {
		NodeList nl = XmlUtilities.selectNodes(_action, "*[not(self::DataLayout)]");
		JSONObject chartDefinition = new JSONObject();
		int length = nl.getLength();
		for (int i = 0; i < length; i++) {
			String key = nl.item(i).getNodeName();
			JSONObject json = readNodes(nl.item(i));
			if (json.has(key)) {
				chartDefinition.put(key, json.get(key));
			} else {
				chartDefinition.put(key, json);
			}
		}
		return chartDefinition;
	}

	protected JSONObject readNodes(Node node) {
		int length;
		JSONObject result = new JSONObject();
		String key = node.getNodeName();
		NamedNodeMap attributes = node.getAttributes();
		if ((attributes != null) && (attributes.getLength() > 0)) {
			length = attributes.getLength();
			for (int i = 0; i < length; i++) {
				if ("value".equalsIgnoreCase(attributes.item(i).getNodeName())) {
					String value = attributes.item(i).getNodeValue();
					if ((value != null) && value.contains("[DataLayout.")) {
						String dataKey = value.replace("[DataLayout.", "").replace("]", "");
						result.put(key, _dataLayouts.get(dataKey));
					} else {
						result.put(key, JSONObject.stringToValue(value));
					}
				}
			}
		}

		// check for child ArrayItem nodes
		NodeList arrayNodes = XmlUtilities.selectNodes(node, "ArrayItem");
		if ((arrayNodes != null) && (arrayNodes.getLength() > 0)) {
			length = arrayNodes.getLength();
			JSONArray jsonArray = new JSONArray();
			for (int i = 0; i < length; i++) {
				JSONObject childJson = readNodes(arrayNodes.item(i));
				jsonArray.put(childJson);
			}
			result.put(key, jsonArray);
		}

		// check for child nodes
		NodeList childNodes = XmlUtilities.selectNodes(node, "*[not(self::ArrayItem)]");
		length = childNodes.getLength();
		for (int i = 0; i < length; i++) {
			String childKey = childNodes.item(i).getNodeName();
			JSONObject childJson = readNodes(childNodes.item(i));
			if (childJson.has(childKey)) {
				result.put(childKey, childJson.get(childKey));
			} else {
				result.put(childKey, childJson);
			}
		}

		return result;
	}
}
