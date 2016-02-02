package com.fanniemae.automation.data;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.automation.SessionManager;
import com.fanniemae.automation.common.DataStream;
import com.fanniemae.automation.common.FileUtilities;
import com.fanniemae.automation.common.XmlUtilities;
import com.fanniemae.automation.data.connectors.BinaryFileConnector;
import com.fanniemae.automation.data.connectors.DataConnector;
import com.fanniemae.automation.data.connectors.DataSetConnector;
import com.fanniemae.automation.data.connectors.DelimitedConnector;
import com.fanniemae.automation.data.connectors.DirectoryConnector;
import com.fanniemae.automation.data.connectors.SqlConnector;
import com.fanniemae.automation.data.transforms.DataTransform;
import com.fanniemae.automation.datafiles.DataWriter;

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
	protected int _ProcessingGroupsCount = 0;

	protected boolean _CacheData = true;

	protected Element _DataSource;
	protected Element _Connection;

	protected Map<Integer, Map<Integer, DataTransform>> _ProcessingGroups = new HashMap<Integer, Map<Integer, DataTransform>>();

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
		String dataFilename = FileUtilities.getDataFilename(_StagingPath, dataSource, _Connection);
		DataStream dataStream = null;
		_DataSource = dataSource;
		defineProcessingGroups();
		for (int iGroup = 0; iGroup < _ProcessingGroupsCount; iGroup++) {
			Map<Integer, DataTransform> dataOperations = _ProcessingGroups.get(iGroup);
			int operationCount = dataOperations.size();

			// Some data operations require access to then entire data stream
			// they cannot be combined with other operations.
			if ((operationCount == 1) && dataOperations.get(0).isTableLevel()) {
				dataStream = dataOperations.get(0).processDataStream(dataStream, _Session.getMemoryLimit());
				continue;
			}

			// These operations can be combined - multiple operations during one
			// pass through the data stream.
			try (DataConnector dc = getConnector(dataStream); DataWriter dw = new DataWriter(dataFilename, 0, false)) {
				dc.open();
				String[][] schema = dc.getDataSourceSchema();
				if (operationCount > 0) {
					// Update the schema based on the operations within this
					// group.
					for (int i = 0; i < operationCount; i++) {
						schema = dataOperations.get(i).UpdateSchema(schema);
					}
				}
				dw.setDataColumns(schema);
				long rowCount = 0;
				while (!dc.eof()) {
					Object[] aValues = dc.getDataRow();
					if (operationCount > 0) {
						for (int i = 0; i < operationCount; i++) {
							aValues = dataOperations.get(i).processDataRow(aValues);
						}
					}

					dw.writeDataRow(aValues);
					rowCount++;
				}
				Calendar calendarExpires = Calendar.getInstance();
				calendarExpires.add(Calendar.MINUTE, 30);
				dw.setFullRowCount(rowCount); // dc.getFullRowCount(_lFullRowCount));
				dw.setBufferFirstRow(1); // dc.getBufferFirstRow());
				dw.setBufferLastRow(rowCount); // dc.getBufferLastRow());
				dw.setBufferExpires(calendarExpires.getTime());
				dw.setFullRowCountKnown(true); // dc.getFullRowCountKnown());
				dw.close();
				dc.close();
				dataStream = dw.getDataStream();
				_Session.addLogMessage("", "Data Returned", String.format("%,d rows (%,d bytes in %s)", rowCount, dataStream.getSize(), dataStream.IsMemory() ? "memorystream" : "filestream"));
			} catch (IOException e) {
				_Session.addErrorMessage(e);
			}
		}
		return dataStream;
	}

	protected DataConnector getConnector(DataStream inputStream) {
		if (inputStream != null) {
			return new BinaryFileConnector(_Session, inputStream, false);
		}

		String connectorType = _DataSource.getAttribute("Type").toLowerCase();
		switch (connectorType) {
		case "sql":
			return new SqlConnector(_Session, _DataSource, false);
		case "directory":
			return new DirectoryConnector(_Session, _DataSource, false);
		case "delimited":
			return new DelimitedConnector(_Session, _DataSource, false);
		case "dataset":
			return new DataSetConnector(_Session, _DataSource, false);
		}
		return null;
	}

	protected void defineProcessingGroups() {
		NodeList nlTransforms = XmlUtilities.selectNodes(_DataSource, "*");
		ExecutionPlanner planner = new ExecutionPlanner(_Session);
		_ProcessingGroups = planner.getExecutionPlan(nlTransforms);
		_ProcessingGroupsCount = _ProcessingGroups.size();
	}

}
