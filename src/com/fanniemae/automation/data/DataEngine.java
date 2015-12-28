package com.fanniemae.automation.data;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

import org.w3c.dom.Element;

import com.fanniemae.automation.SessionManager;
import com.fanniemae.automation.common.DataStream;
import com.fanniemae.automation.common.FileUtilities;
import com.fanniemae.automation.data.connectors.DataConnector;
import com.fanniemae.automation.data.connectors.SqlConnector;
import com.fanniemae.automation.datafiles.DataWriter;
import com.fanniemae.automation.datafiles.lowlevel.DataFileEnums;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-22
 * 
 */
public class DataEngine {
	protected static final long ONE_MINUTE_IN_MILLIS = 60000; // milliseconds
	
	protected SessionManager _Session;

	protected String _StagingPath;
	protected int _MemoryLimit; // in Megabytes
	protected boolean _CacheData = true;

	protected Element _DataSource;
	protected Element _Connection;
	
	protected Map<DataFileEnums.BinaryFileInfo, Object> _aDataFileHeader = null;

	public DataEngine(SessionManager session) {
		_Session = session;
		_MemoryLimit = _Session.getMemoryLimit();
		_StagingPath = _Session.getStagingPath();
	}

	public int getMemoryLimit() {
		return _MemoryLimit;
	}

	public void setMemoryLimit(int value) {
		_MemoryLimit = (value < 0) ? Integer.MAX_VALUE : value;
	}

	public String getStagingPath() {
		return _StagingPath;
	}

	public void setStagingPath(String path) {
		if (FileUtilities.isInvalidDirectory(path))
			throw new RuntimeException(String.format("Staging directory %s does not exist.", path));

		_StagingPath = path.endsWith(File.separator) ? path : path + File.separator;
	}

	public DataStream getData(Element dataSource) {
		String sDataFilename = FileUtilities.getDataFilename(_StagingPath, dataSource, _Connection);
		DataStream dataStream = null;
		_DataSource = dataSource;
		try (DataConnector dc = CreateConnector(); DataWriter dw = new DataWriter(sDataFilename, 0, false)) {
			dc.open();
			String[][] aSchema = dc.getDataSourceSchema();
			dw.SetupDataColumns(aSchema);
			long lRowCount = 0;
			while(!dc.eof()) {
				Object[]  aValues = dc.getDataRow();
				dw.WriteDataRow(aValues);
				lRowCount++;
			}
			Date dtExpires = new Date();
			dtExpires = new Date(dtExpires.getTime() + (30 * ONE_MINUTE_IN_MILLIS));
            dw.setFullRowCount(lRowCount); //dc.getFullRowCount(_lFullRowCount));
            dw.setBufferFirstRow(1); //dc.getBufferFirstRow());
            dw.setBufferLastRow(lRowCount); //dc.getBufferLastRow());
            dw.setBufferExpires(dtExpires);
            dw.setFullRowCountKnown(true); //dc.getFullRowCountKnown());
            dw.close();
            _aDataFileHeader = dw.getHeader();
            if (dw.IsFilestream()) {
                dataStream = new DataStream(sDataFilename);
            } else {
                dataStream = new DataStream(dw.getDataBuffer());
            }
			dc.close();
			dw.close();
			_Session.addLogMessage("", "Data Returned", String.format("%,d rows (%,d bytes in %s)",lRowCount,dataStream.getSize(), dataStream.IsMemory() ? "memorystream" : "filestream"));
		} catch (IOException e) {
			_Session.addErrorMessage(e);
		}
		return dataStream;
	}

	protected DataConnector CreateConnector() {
		String sType = _DataSource.getAttribute("Type").toLowerCase();
		switch (sType) {
		case "sql":
			return new SqlConnector(_Session, _DataSource, false);
		}
		return null;
	}

}
