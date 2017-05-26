/**
 *  
 * Copyright (c) 2015 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.data;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.DataStream;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.XmlUtilities;
import com.fanniemae.ezpie.data.connectors.DataConnector;
import com.fanniemae.ezpie.data.connectors.DataSetConnector;
import com.fanniemae.ezpie.data.connectors.DelimitedConnector;
import com.fanniemae.ezpie.data.connectors.DirectoryConnector;
import com.fanniemae.ezpie.data.connectors.ExcelConnector;
import com.fanniemae.ezpie.data.connectors.RestConnector;
import com.fanniemae.ezpie.data.connectors.SqlConnector;
import com.fanniemae.ezpie.data.transforms.DataTransform;
import com.fanniemae.ezpie.datafiles.DataWriter;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2015-12-22
 * 
 */

public class DataEngine {
	protected SessionManager _session;

	protected String _stagingPath;
	protected String[][] _schema;

	protected int _memoryLimit; // in Megabytes
	protected int _processingGroupsCount = 0;

	protected boolean _cacheData = true;

	protected Element _dataSource;
	protected Element _connection;

	protected Map<Integer, Map<Integer, DataTransform>> _processingGroups = new HashMap<Integer, Map<Integer, DataTransform>>();

	public DataEngine(SessionManager session) {
		_session = session;
		_memoryLimit = _session.getMemoryLimit();
		_stagingPath = _session.getStagingPath();
	}

	public int getMemoryLimit() {
		return _memoryLimit;
	}

	public void setMemoryLimit(int value) {
		_memoryLimit = (value < 0) ? Integer.MAX_VALUE : value;
	}

	public String getStagingPath() {
		return _stagingPath;
	}
	
	public String[][] getSchema() {
		return _schema;
	}

	public void setStagingPath(String path) {
		if (FileUtilities.isInvalidDirectory(path))
			throw new RuntimeException(String.format("Staging directory %s does not exist.", path));

		_stagingPath = path.endsWith(File.separator) ? path : path + File.separator;
	}

	public DataStream getData(Element dataSource) {
		String dataFilename = FileUtilities.getDataFilename(_stagingPath, dataSource, _connection);
		DataStream dataStream = null;
		_dataSource = dataSource;
		defineProcessingGroups();
		for (int iGroup = 0; iGroup < _processingGroupsCount; iGroup++) {
			Map<Integer, DataTransform> dataOperations = _processingGroups.get(iGroup);
			int operationCount = dataOperations.size();

			// Some data operations require access to then entire data stream
			// they cannot be combined with other operations.
			if ((operationCount == 1) && dataOperations.get(0).isTableLevel()) {
				_session.addLogMessage("", String.format("Processing Group #%d of %d", iGroup + 1, _processingGroupsCount), "");
				dataOperations.get(0).addTransformLogMessage();
				dataStream = dataOperations.get(0).processDataStream(dataStream, _session.getMemoryLimit());
				continue;
			}

			// These operations can be combined - multiple operations during one
			// pass through the data stream.
			try (DataConnector dc = getConnector(dataStream); DataWriter dw = new DataWriter(dataFilename, 0, false)) {
				dc.open();
				String[][] schema = dc.getDataSourceSchema();
				if (operationCount > 0) {
					_session.addLogMessage("", String.format("Processing Group #%d of %d", iGroup + 1, _processingGroupsCount), "");
					// Update the schema based on the operations within this
					// group.
					for (int i = 0; i < operationCount; i++) {
						dataOperations.get(i).addTransformLogMessage();
						schema = dataOperations.get(i).UpdateSchema(schema);
					}
				}
				_schema = schema;
				dw.setDataColumns(schema);
				long rowCount = 0;
				while (!dc.eof()) {
					Object[] aValues = dc.getDataRow();
					if (aValues == null) 
						continue;
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
				_session.addLogMessage("", "Data Returned", String.format("%,d rows (%,d bytes in %s)", rowCount, dataStream.getSize(), dataStream.IsMemory() ? "memorystream" : "filestream"));
			} catch (IOException e) {
				_session.addErrorMessage(e);
			}
		}
		return dataStream;
	}

	protected DataConnector getConnector(DataStream inputStream) {
		if (inputStream != null) {
			return new DataSetConnector(_session, inputStream, false);
		}

		String connectorType = _dataSource.getAttribute("Type").toLowerCase();
		switch (connectorType) {
		case "":
			throw new RuntimeException("DataSource Type attribute not defined.");
		case "sql":
			return new SqlConnector(_session, _dataSource, false);
		case "directory":
			return new DirectoryConnector(_session, _dataSource, false);
		case "delimited":
			return new DelimitedConnector(_session, _dataSource, false);
		case "dataset":
			return new DataSetConnector(_session, _dataSource, false);
		case "rest":
			return new RestConnector(_session, _dataSource, false);
		case "excel":
			return new ExcelConnector(_session, _dataSource, false);
		default:
			throw new RuntimeException(String.format("Requested DataSource Type=%s not currently supported.", connectorType));
		}
	}

	protected void defineProcessingGroups() {
		NodeList nlTransforms = XmlUtilities.selectNodes(_dataSource, "*");
		ExecutionPlanner planner = new ExecutionPlanner(_session);
		_processingGroups = planner.getExecutionPlan(nlTransforms);
		_processingGroupsCount = _processingGroups.size();
	}

}
