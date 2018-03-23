/**
 *  
 * Copyright (c) 2017 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.common;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.fanniemae.ezpie.datafiles.DataReader;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2017-08-15
 * 
 */

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
						jsonDataRow.put(columnNames[i], new BigDecimal((double) dataRow[i]).toPlainString());
					} else if (dataTypes[i] == DataType.IntegerData) {
						jsonDataRow.put(columnNames[i], (int) dataRow[i]);
					} else if (dataTypes[i] == DataType.FloatData) {
						jsonDataRow.put(columnNames[i], new BigDecimal((double) dataRow[i]).toPlainString());
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
			throw new PieException(String.format("Error while converting %s datastream into JSON. %s", name, ex.getMessage()), ex);
		}

		return dataSet;
	}
	
	public static String writeJsonFile(String responseStr, String filename) {
		Object responseJSON = new JSONTokener(responseStr).nextValue();
		String jsonString = "";
		if (responseJSON instanceof JSONObject) {
			jsonString = ((JSONObject) responseJSON).toString(2);
		} else if (responseJSON instanceof JSONArray) {
			jsonString = ((JSONArray) responseJSON).toString(2);
		}

		// write returned JSON to file in logs folder
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "utf-8"))) {
			writer.write(jsonString);
		} catch (IOException ex) {
			// TODO Auto-generated catch block
			throw new PieException("Error while trying to write REST response to file: " + ex.getMessage(), ex);
		}

		return filename;
	}
	
	public static void jsonSchema(String s) {
		if ((s == null) || s.isEmpty()) {
			return;
		}
		
		JSONObject jsonObject;
		if (s.startsWith("[")) {
			jsonObject = new JSONObject();
			jsonObject.put("rows", new JSONArray(s));
		} else {
			jsonObject = new JSONObject(s);
		}
		
		List<String> columnNames = new ArrayList<String>();
		// Begin scan for resulting data columns of flattened json.
		Iterator<?> keys = jsonObject.keys();
//		foreach(String key : keys) {
//			
//		}
		int numKeys = jsonObject.length();
		
		for (int i=0;i< numKeys;i++) {
			String key = jsonObject.names().getString(i);
			 
			
			
		}
	}
}
