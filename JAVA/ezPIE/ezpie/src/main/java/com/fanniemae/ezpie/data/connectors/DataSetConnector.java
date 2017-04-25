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

package com.fanniemae.ezpie.data.connectors;

import java.io.IOException;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.DataStream;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.datafiles.DataReader;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-01-27
 * 
 */

public class DataSetConnector extends DataConnector {

	protected String _dataSetName;
	
	protected DataStream _dataStream;
	
	protected DataReader _dr;
	
	public DataSetConnector(SessionManager session, Element dataSource, Boolean isSchemaOnly) {
		super(session, dataSource, isSchemaOnly);
		
		_dataSetName = _session.getAttribute(dataSource, "DataSetName");
		if (StringUtilities.isNullOrEmpty(_dataSetName)) {
			throw new RuntimeException("DataSource.DataSet is missing the required DataSetName.");
		}
		_dataStream = _session.getDataStream(_dataSetName);
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
