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

package com.fanniemae.ezpie.actions;

import java.util.Date;
import java.util.HashMap;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.DataStream;
import com.fanniemae.ezpie.common.DateUtilities;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.common.ProcessActions;
import com.fanniemae.ezpie.common.XmlUtilities;
import com.fanniemae.ezpie.datafiles.DataReader;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2015-12-16
 * 
 */

public class DataLoop extends Action {

	public DataLoop(SessionManager session, Element action) {
		super(session, action, false);
	}

	@Override
	public String executeAction(HashMap<String, String> dataTokens) {
		String dataSetName = requiredAttribute("DataSetName");
		DataStream _dataStream = _session.getDataStream(dataSetName);
		int rowNumber = 0;
		try (DataReader dr = new DataReader(_dataStream)) {
			String[] colNames = dr.getColumnNames();
			DataType[] dataTypes = dr.getDataTypes();
			while (!dr.eof()) {
				Object[] dataRow = dr.getDataRow();

				HashMap<String, String> currentDataTokens = new HashMap<String, String>();
				// Combine the tokens provided via the parameter with this row.
				if (dataTokens != null) {
					currentDataTokens.putAll(dataTokens);
				}

				// Load values into dataTokens
				for (int i = 0; i < colNames.length; i++) {
					if (dataRow[i] == null) {
						currentDataTokens.put(colNames[i], "");
					} else if (dataTypes[i] == DataType.DateData) {
						currentDataTokens.put(colNames[i], DateUtilities.toIsoString((Date) dataRow[i]));
					} else if (dataTypes[i] == DataType.StringData) {
						currentDataTokens.put(colNames[i], (String) dataRow[i]);
					} else {
						currentDataTokens.put(colNames[i], dataRow[i].toString());
					}
				}

				NodeList childActions = XmlUtilities.selectNodes(_action, "*");
				ProcessActions.run(_session, childActions, currentDataTokens);
				rowNumber++;
			}
			dr.close();
			// _session.addLogMessage("", "Data", String.format("%,d rows of data written.", iRowCount));
			// _session.addLogMessage("", "Completed", String.format("Data saved to %s", _outputFilename));
		} catch (Exception e) {
			RuntimeException ex = new PieException(String.format("Error while looping through the data (row %,d)", rowNumber), e);
			throw ex;
		}
		return null;
	}

}
