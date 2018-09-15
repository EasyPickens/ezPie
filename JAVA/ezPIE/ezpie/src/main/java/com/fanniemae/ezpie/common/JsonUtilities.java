/**
 *  
 * Copyright (c) 2017 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.common;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;

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

			// Add an array of data set column names to the output.
			dataSet.put("ColumnNames", columnNames);

			// Add an array of the data set column types to the output.
			dataSet.put("ColumnType", dataTypesToJsonTypes(dataTypes));

			while (!dr.eof()) {
				Object[] dataRow = dr.getDataRow();
				JSONObject jsonDataRow = new JSONObject();

				for (int i = 0; i < columnNames.length; i++) {
					if (dataRow[i] == null) {
						jsonDataRow.put(columnNames[i], JSONObject.NULL);
					} else if (dataTypes[i] == DataType.DateData) {
						jsonDataRow.put(columnNames[i], DateUtilities.toIsoString((Date) dataRow[i]));
					} else {
						jsonDataRow.put(columnNames[i], dataRow[i]);
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

	public static String[] dataTypesToJsonTypes(DataType[] dataTypes) {
		String[] columnTypes = new String[dataTypes.length];
		for (int i = 0; i < dataTypes.length; i++) {
			switch (dataTypes[i]) {
			case DateData:
				columnTypes[i] = "DateTime";
				break;
			case BooleanData:
				columnTypes[i] = "Boolean";
				break;
			case BigDecimalData:
			case ByteData:
			case DoubleData:
			case FloatData:
			case IntegerData:
			case LongData:
			case ShortData:
				columnTypes[i] = "Numeric";
				break;
			default:
				columnTypes[i] = "String";
				break;
			}
		}
		return columnTypes;
	}

}
