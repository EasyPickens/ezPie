package com.fanniemae.automation.data.connectors;

import java.io.IOException;

import com.fanniemae.automation.SessionManager;
import com.fanniemae.automation.common.DataStream;
import com.fanniemae.automation.datafiles.DataReader;

public class BinaryFileConnector extends DataConnector {

	protected DataReader _dr;
	protected DataStream _inputStream;

	public BinaryFileConnector(SessionManager session, DataStream inputStream, Boolean isSchemaOnly) {
		super(session, null, isSchemaOnly);
		_inputStream = inputStream;
	}

	// public BinaryFileConnector(SessionManager session, Element dataSource,
	// Boolean isSchemaOnly) {
	// super(session, dataSource, isSchemaOnly);
	// // TODO Auto-generated constructor stub
	// }

	@Override
	public Boolean open() {
		try {
			_dr = new DataReader(_inputStream);
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
