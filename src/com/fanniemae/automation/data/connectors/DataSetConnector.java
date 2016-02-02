package com.fanniemae.automation.data.connectors;

import java.io.IOException;

import org.w3c.dom.Element;

import com.fanniemae.automation.SessionManager;
import com.fanniemae.automation.common.DataStream;
import com.fanniemae.automation.common.StringUtilities;
import com.fanniemae.automation.datafiles.DataReader;

/**
 * 
 * @author Richard Monson
 * @since 2016-02-01
 * 
 */
public class DataSetConnector extends DataConnector {

	protected String _DataSetID;
	
	protected DataStream _dataStream;
	
	protected DataReader _dr;
	
	public DataSetConnector(SessionManager session, Element dataSource, Boolean isSchemaOnly) {
		super(session, dataSource, isSchemaOnly);
		
		_DataSetID = _Session.getAttribute(dataSource, "DataSetID");
		if (StringUtilities.isNullOrEmpty(_DataSetID)) {
			throw new RuntimeException("DataSource.DataSet is missing the required DataSetID.");
		}
		_dataStream = _Session.getDataStream(_DataSetID);
	}

	@Override
	public Boolean open() {
		try {
			_dr = new DataReader(_dataStream);
			_DataSchema = _dr.getSchema();
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
