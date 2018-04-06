/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.actions;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.DataStream;
import com.fanniemae.ezpie.common.DateUtilities;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.common.XmlUtilities;
import com.fanniemae.ezpie.datafiles.DataReader;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-07-15
 * 
 */

public class Tokens extends Action {

	protected Boolean _fixedTokens = false;

	public Tokens(SessionManager session, Element action) {
		super(session, action, false);
	}

	@Override
	public String executeAction(HashMap<String, String> dataTokens) {
		_session.setDataTokens(dataTokens);
		NodeList nl = XmlUtilities.selectNodes(_action, "DataSource");
		int length = nl.getLength();
		for (int i = 0; i < length; i++) {
			Element child = (Element) nl.item(i);
			if ((child != null) && "DataSource".equalsIgnoreCase(child.getNodeName())) {
				readData(child, dataTokens);
			}
		}
		_session.addTokens(_action);
		_session.clearDataTokens();
		return null;
	}

	protected void readData(Element child, HashMap<String, String> dataTokens) {
		String dataSetName = requiredAttribute(child, "DataSetName");
		DataStream dataStream = _session.getDataStream(dataSetName);

		try (DataReader dr = new DataReader(dataStream)) {
			String[] columnNames = dr.getColumnNames();
			DataType[] outputColumnDataTypes = dr.getDataTypes();

			if (!dr.eof()) {
				Map<String, String> newTokens = new HashMap<String, String>();
				Object[] dataRow = dr.getDataRow();
				for (int i = 0; i < dataRow.length; i++) {
					String value = "";
					if (dataRow[i] == null) {
						value = "";
					} else if (outputColumnDataTypes[i] == DataType.DateData) {
						value = DateUtilities.toIsoString((Date) dataRow[i]);
					} else {
						value = dataRow[i].toString();
					}
					newTokens.put(columnNames[i], value);
				}
				_session.addTokens(dataSetName, newTokens);
			}

			dr.close();
		} catch (Exception e) {
			throw new PieException("Error while trying to convert the data into tokens. " + e.getMessage(), e);
		}
		_session.clearDataTokens();
	}

}
