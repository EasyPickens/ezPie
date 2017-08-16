package com.fanniemae.ezpie.common;

import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fanniemae.ezpie.datafiles.DataReader;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

public class JsonUtilities {

	private JsonUtilities() {
	}

	public static JSONObject convert(String name, DataStream ds) {
		JSONObject dataSet = new JSONObject();
		dataSet.put("Name", name);
		JSONArray data = new JSONArray();

		try (DataReader dr = new DataReader(ds)) {
			String[] columnNames = dr.getColumnNames();
			DataType[] dataTypes = dr.getDataTypes();
			while (!dr.eof()) {
				Object[] dataRow = dr.getDataRow();
				JSONObject jsonDataRow = new JSONObject();

				for (int i = 0; i < columnNames.length; i++) {
					if (dataRow[i] == null) {
						jsonDataRow.put(columnNames[i], "");
					} else if (dataTypes[i] == DataType.DateData) {
						jsonDataRow.put(columnNames[i], DateUtilities.toIsoString((Date) dataRow[i]));
					} else if (dataTypes[i] == DataType.StringData) {
						jsonDataRow.put(columnNames[i], dataRow[i]);
					} else if (dataTypes[i] == DataType.DoubleData) {
						jsonDataRow.put(columnNames[i], (double) dataRow[i]);
					} else if (dataTypes[i] == DataType.IntegerData) {
						jsonDataRow.put(columnNames[i], (int) dataRow[i]);
					} else if (dataTypes[i] == DataType.FloatData) {
						jsonDataRow.put(columnNames[i], (float) dataRow[i]);
					} else if (dataTypes[i] == DataType.LongData) {
						jsonDataRow.put(columnNames[i], (long) dataRow[i]);
					} else if (dataTypes[i] == DataType.ShortData) {
						jsonDataRow.put(columnNames[i], (int) dataRow[i]);
					} else {
						jsonDataRow.put(columnNames[i], dataRow[i].toString());
					}
				}
				data.put(jsonDataRow);
			}
			dr.close();
			dataSet.put("Data", data);
		} catch (Exception ex) {
			throw new RuntimeException(String.format("Error while converting %s datastream into JSON. %s", name, ex.getMessage()));
		}

		return dataSet;
	}
}
