package com.fanniemae.automation.data;

import java.io.File;

import org.w3c.dom.Element;

import com.fanniemae.automation.SessionManager;
import com.fanniemae.automation.common.FileUtilities;
import com.fanniemae.automation.data.connectors.DataConnector;
import com.fanniemae.automation.data.connectors.SqlConnector;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-22
 * 
 */
public class DataEngine {
	protected SessionManager _Session;

	protected String _StagingPath;
	protected int _MemoryLimit; // in Megabytes
	protected boolean _CacheData = true;

	protected Element _DataSource;
	protected Element _Connection;

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
	
	public String getData(Element dataSource) {
		_DataSource = dataSource;
		try (DataConnector dc = CreateConnector()) {
			dc.open();
			dc.close();
		}
		return "";
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
