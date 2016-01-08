package com.fanniemae.automation.data;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fanniemae.automation.SessionManager;
import com.fanniemae.automation.common.DataStream;
import com.fanniemae.automation.common.FileUtilities;
import com.fanniemae.automation.common.XmlUtilities;
import com.fanniemae.automation.data.connectors.DataConnector;
import com.fanniemae.automation.data.connectors.SqlConnector;
import com.fanniemae.automation.data.transforms.DataTransform;
import com.fanniemae.automation.data.transforms.TimePeriodColumn;
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

	protected Map<Integer, Map<Integer, DataTransform>> _ProcessingGroups = new HashMap<Integer, Map<Integer, DataTransform>>();

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
		DefineProcessingGroups();
		try (DataConnector dc = CreateConnector(); DataWriter dw = new DataWriter(sDataFilename, 0, false)) {
			dc.open();
			String[][] aSchema = dc.getDataSourceSchema();
			// Update the schema based on the operations within this group.
			for(int i=0;i<_ProcessingGroups.get(0).size();i++) {
				aSchema = _ProcessingGroups.get(0).get(i).UpdateSchema(aSchema);
			}
			dw.SetupDataColumns(aSchema);
			long lRowCount = 0;
			while (!dc.eof()) {
				Object[] aValues = dc.getDataRow();
				for(int i=0;i<_ProcessingGroups.get(0).size();i++) {
					aValues = _ProcessingGroups.get(0).get(i).processDataRow(aValues);
				}
				
				dw.WriteDataRow(aValues);
				lRowCount++;
			}
			Date dtExpires = new Date();
			dtExpires = new Date(dtExpires.getTime() + (30 * ONE_MINUTE_IN_MILLIS));
			dw.setFullRowCount(lRowCount); // dc.getFullRowCount(_lFullRowCount));
			dw.setBufferFirstRow(1); // dc.getBufferFirstRow());
			dw.setBufferLastRow(lRowCount); // dc.getBufferLastRow());
			dw.setBufferExpires(dtExpires);
			dw.setFullRowCountKnown(true); // dc.getFullRowCountKnown());
			dw.close();
			_aDataFileHeader = dw.getHeader();
			if (dw.IsFilestream()) {
				dataStream = new DataStream(sDataFilename);
			} else {
				dataStream = new DataStream(dw.getDataBuffer());
			}
			dc.close();
			dw.close();
			_Session.addLogMessage("", "Data Returned", String.format("%,d rows (%,d bytes in %s)", lRowCount, dataStream.getSize(), dataStream.IsMemory() ? "memorystream" : "filestream"));
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

	protected void DefineProcessingGroups() {
		NodeList nlTransforms = XmlUtilities.selectNodes(_DataSource, "*");
		if (nlTransforms.getLength() == 0)
			return;

		Map<Integer, DataTransform> aCurrentGroup = new HashMap<Integer, DataTransform>();
		int iLen = nlTransforms.getLength();
		for (int i = 0; i < iLen; i++) {
			Element eleTransform = (Element) nlTransforms.item(i);
			String sName = eleTransform.getNodeName();
			switch (sName) {
			case "TimePeriodColumn":
				aCurrentGroup.put(aCurrentGroup.size(), new TimePeriodColumn(_Session, eleTransform));
				break;
			}
		}
		_ProcessingGroups.put(_ProcessingGroups.size(), aCurrentGroup);
	}

}
