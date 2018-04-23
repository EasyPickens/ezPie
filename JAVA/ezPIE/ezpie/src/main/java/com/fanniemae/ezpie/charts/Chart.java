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
import com.fanniemae.ezpie.actions.Action;
import com.fanniemae.ezpie.common.ArrayUtilities;
import com.fanniemae.ezpie.common.DateUtilities;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.common.XmlUtilities;
import com.fanniemae.ezpie.datafiles.DataReader;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

public class Chart extends Action {

	protected Map<String, List<Object>> _dataLayouts = new HashMap<String, List<Object>>();
	protected Map<String, int[]> _dataColumnIndexes = new HashMap<String, int[]>();

	// protected List<Object> _labels = new ArrayList<Object>();
	// protected Map<Integer, List<Object>> _dataSets = new HashMap<Integer, List<Object>>();

	protected String[] _columnNames;
	protected DataType[] _dataTypes;
	protected JSONArray _fullData = new JSONArray();

	public Chart(SessionManager session, Element action) {
		super(session, action, true);
	}

	@Override
	public String executeAction(HashMap<String, String> dataTokens) {
		_session.setDataTokens(dataTokens);
		String dataSetName = requiredAttribute("DataSetName");
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
				String name = requiredAttribute(dataLayouts.item(i), "Name");
				String[] columnNames = StringUtilities.split(requiredAttribute(dataLayouts.item(i), "DataRow"));
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
		chartJson.put("Name", requiredAttribute("Name"));
		chartJson.put("Chart", chartSettings);
		chartJson.put("Data",_fullData);
		chartJson.put("ColumnNames",_columnNames);
		
		
		return chartJson.toString();
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
			chartDefinition.put(nl.item(i).getNodeName(), readNode2(nl.item(i)));
		}
		return chartDefinition;
	}

	protected String convertToJsonX() {
		JSONObject chartJson = new JSONObject();
		JSONObject options = new JSONObject();
		JSONArray datasets = new JSONArray();
		JSONObject data = new JSONObject();

		chartJson.put("type", "line");
		chartJson.put("data", data);
		chartJson.put("options", options);
		// data.put("labels", _labels);
		data.put("datasets", datasets);

		NodeList dataNodes = XmlUtilities.selectNodes(_action, "Data");
		int length = dataNodes.getLength();
		for (int i = 0; i < length; i++) {
			JSONObject metricValues = new JSONObject();
			// metricValues.put("data", _dataSets.get(i).toArray());
			NodeList additionalSettings = XmlUtilities.selectNodes(dataNodes.item(i), "Set");
			int count = additionalSettings.getLength();
			for (int x = 0; x < count; x++) {
				Node currentNode = additionalSettings.item(x);
				NamedNodeMap attributeList = currentNode.getAttributes();
				int attributeCount = attributeList.getLength();
				for (int y = 0; y < attributeCount; y++) {
					metricValues.put(attributeList.item(y).getNodeName(), attributeList.item(y).getNodeValue());
				}
			}
			datasets.put(metricValues);
		}

		// Read any defined options
		NodeList optionNodes = XmlUtilities.selectNodes(_action, "Options");
		length = optionNodes.getLength();
		for (int i = 0; i < length; i++) {
			NodeList childNodes = XmlUtilities.selectNodes(optionNodes.item(i), "*");
			int childcount = childNodes.getLength();
			for (int x = 0; x < childcount; x++) {
				Node optionChild = childNodes.item(x);
				NamedNodeMap attributeList = optionChild.getAttributes();
				int attributeCount = attributeList.getLength();
				JSONObject current = new JSONObject();
				for (int y = 0; y < attributeCount; y++) {
					current.put(attributeList.item(y).getNodeName(), attributeList.item(y).getNodeValue());
				}
				options.put(optionChild.getNodeName(), current);
			}

		}

		// System.out.println(chartJson.toString());
		NodeList test2 = XmlUtilities.selectNodes(_action, "dataX");
		for (int i = 0; i < test2.getLength(); i++) {
			JSONObject aa = readNode2(test2.item(i));
			System.out.println(aa);
		}

		// NodeList test = XmlUtilities.selectNodes(_action, "*[self::data or self::options]");
		// for(int i=0;i<test.getLength();i++) {
		// JSONObject xx = readNode(test.item(i));
		//
		// // Node options2 = XmlUtilities.selectSingleNode(_action, "Options2");
		// // JSONObject xx = readNode(options2);
		//
		// System.out.println(xx);
		// }
		return chartJson.toString();
	}

	protected JSONObject readNode(Node node) {
		int length;
		JSONObject result = new JSONObject();
		String key = node.getNodeName();
		NamedNodeMap attr = node.getAttributes();
		if ((attr != null) && (attr.getLength() > 0)) {
			length = attr.getLength();
			for (int i = 0; i < length; i++) {
				result.put(attr.item(i).getNodeName(), JSONObject.stringToValue(attr.item(i).getNodeValue()));
			}
		}

		// check for child nodes
		NodeList childNodes = XmlUtilities.selectNodes(node, "*");
		length = childNodes.getLength();
		for (int i = 0; i < length; i++) {
			String childKey = childNodes.item(i).getNodeName();
			JSONObject childJson = readNode(childNodes.item(i));
			if ("ArrayItem".equalsIgnoreCase(childKey)) {
				JSONArray ja = new JSONArray();
				ja.put(childJson);
				result.put(key, ja);
			} else {
				if (childJson.has(childKey)) {
					result.put(childKey, childJson.get(childKey));
				} else {
					result.put(childKey, childJson);
				}
			}
		}

		return result;
	}

	protected JSONObject readNode2(Node node) {
		int length;
		JSONObject result = new JSONObject();
		String key = node.getNodeName();
		NamedNodeMap attr = node.getAttributes();
		if ((attr != null) && (attr.getLength() > 0)) {
			length = attr.getLength();
			for (int i = 0; i < length; i++) {
				if ("value".equalsIgnoreCase(attr.item(i).getNodeName())) {
					String value = attr.item(i).getNodeValue();
					if (value.contains("[DataLayout.")) {
						String dataKey = value.replace("[DataLayout.", "").replace("]", "");
						result.put(key, _dataLayouts.get(dataKey));
					} else {
						result.put(key, JSONObject.stringToValue(attr.item(i).getNodeValue()));
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
				JSONObject childJson = readNode2(arrayNodes.item(i));
				jsonArray.put(childJson);
			}
			result.put(key, jsonArray);
		}

		// check for child nodes
		NodeList childNodes = XmlUtilities.selectNodes(node, "*[not(self::ArrayItem)]");
		length = childNodes.getLength();
		for (int i = 0; i < length; i++) {
			String childKey = childNodes.item(i).getNodeName();
			JSONObject childJson = readNode2(childNodes.item(i));
			if (childJson.has(childKey)) {
				result.put(childKey, childJson.get(childKey));
			} else {
				result.put(childKey, childJson);
			}
		}

		return result;
	}
}
