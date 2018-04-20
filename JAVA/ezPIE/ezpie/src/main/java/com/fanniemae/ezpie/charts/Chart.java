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
import com.fanniemae.ezpie.common.XmlUtilities;
import com.fanniemae.ezpie.datafiles.DataReader;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

public class Chart extends Action {

	protected List<Object> _labels = new ArrayList<Object>();
	protected Map<Integer, List<Object>> _dataSets = new HashMap<Integer, List<Object>>();
	protected String[] _columnNames;
	protected DataType[] _dataTypes;
	protected int[] _dataColumnIndexes;

	public Chart(SessionManager session, Element action) {
		super(session, action, true);
	}

	@Override
	public String executeAction(HashMap<String, String> dataTokens) {
		_session.setDataTokens(dataTokens);
		String dataSetName = requiredAttribute("DataSetName");
		String labelColumnName = requiredAttribute("LabelColumnName");
		try (DataReader dr = new DataReader(_session.getDataStream(dataSetName))) {
			_columnNames = dr.getColumnNames();
			_dataTypes = dr.getDataTypes();
			int labelIndex = ArrayUtilities.indexOf(_columnNames, labelColumnName);

			// Setup the index array and initialize the data arrays for all the metric columns
			NodeList dataNodes = XmlUtilities.selectNodes(_action, "Data");
			if ((dataNodes == null) || (dataNodes.getLength() < 1)) {
				throw new PieException(String.format("%s element requires at least one child Data element.", _actionName));
			}
			_dataColumnIndexes = new int[dataNodes.getLength()];
			for (int i = 0; i < _dataColumnIndexes.length; i++) {
				String name = requiredAttribute(dataNodes.item(i), "ColumnName");
				int index = ArrayUtilities.indexOf(_columnNames, name);
				if (index < 0) {
					throw new PieException(String.format("%s column was not found in the %s dataset", name, dataSetName));
				}
				_dataColumnIndexes[i] = index;
				_dataSets.put(i, new ArrayList<Object>());
			}

			while (!dr.eof()) {
				Object[] dataRow = dr.getDataRow();
				_labels.add(getMetric(_dataTypes[labelIndex], dataRow[labelIndex]));
				for (int i = 0; i < _dataColumnIndexes.length; i++) {
					_dataSets.get(i).add(getMetric(_dataTypes[_dataColumnIndexes[i]], dataRow[_dataColumnIndexes[i]]));
				}
			}
			dr.close();
		} catch (Exception ex) {
			throw new PieException(String.format("Error while converting %s datastream into chart data. %s", dataSetName, ex.getMessage()), ex);
		}
		_session.clearDataTokens();

		// Assemble the final JSON data and write a file.
		return convertToJson();
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

	protected String convertToJson() {
		JSONObject chartJson = new JSONObject();
		JSONObject options = new JSONObject();
		JSONArray datasets = new JSONArray();
		JSONObject data = new JSONObject();

		chartJson.put("type", "line");
		chartJson.put("data", data);
		chartJson.put("options", options);
		data.put("labels", _labels);
		data.put("datasets", datasets);

		NodeList dataNodes = XmlUtilities.selectNodes(_action, "Data");
		int length = dataNodes.getLength();
		for (int i = 0; i < length; i++) {
			JSONObject metricValues = new JSONObject();
			metricValues.put("data", _dataSets.get(i).toArray());
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
					result.put(key, JSONObject.stringToValue(attr.item(i).getNodeValue()));
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
