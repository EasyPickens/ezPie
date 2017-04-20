/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPIE/wiki/Fannie-Mae-Open-Source-Licensing-Project
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.devtools.pie.data.connectors;

import java.io.IOException;

import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.DataStream;
import com.fanniemae.devtools.pie.common.StringUtilities;
import com.fanniemae.devtools.pie.datafiles.DataReader;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-01-27
 * 
 */

public class DataSetConnector extends DataConnector {

	protected String _dataSetID;
	
	protected DataStream _dataStream;
	
	protected DataReader _dr;
	
	public DataSetConnector(SessionManager session, Element dataSource, Boolean isSchemaOnly) {
		super(session, dataSource, isSchemaOnly);
		
		_dataSetID = _session.getAttribute(dataSource, "DataSetID");
		if (StringUtilities.isNullOrEmpty(_dataSetID)) {
			throw new RuntimeException("DataSource.DataSet is missing the required DataSetID.");
		}
		_dataStream = _session.getDataStream(_dataSetID);
	}
	
	public DataSetConnector(SessionManager session, DataStream dataStream, Boolean isSchemaOnly) {
		super(session, null, isSchemaOnly);
		_dataStream = dataStream;
	}

	@Override
	public Boolean open() {
		try {
			_dr = new DataReader(_dataStream);
			_dataSchema = _dr.getSchema();
		} catch (IOException ex) {
			throw new RuntimeException("Could not open requested data stream.", ex);
		}
		return null;
	}

	@Override
	public Boolean eof() {
		try {
			return _dr.eof();
		} catch (IOException ex) {
			throw new RuntimeException("Error while deteriming value of End-Of-File flag.", ex);
		}
	}

	@Override
	public Object[] getDataRow() {
		try {
			return _dr.getDataRow();
		} catch (IOException ex) {
			throw new RuntimeException("Error while reading the data stream.", ex);
		}
	}

	@Override
	public void close() {
		if (_dr != null) {
			try {
				_dr.close();
			} catch (Exception ex) {
				throw new RuntimeException("Error while trying to close the data stream.", ex);
			}
		}
	}

}
