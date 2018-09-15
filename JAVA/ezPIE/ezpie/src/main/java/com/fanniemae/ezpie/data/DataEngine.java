/**
 *  
 * Copyright (c) 2015 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.Constants;
import com.fanniemae.ezpie.common.DataStream;
import com.fanniemae.ezpie.common.DateUtilities;
import com.fanniemae.ezpie.common.ExceptionUtilities;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.common.XmlUtilities;
import com.fanniemae.ezpie.data.connectors.DataConnector;
import com.fanniemae.ezpie.data.connectors.DataSetConnector;
import com.fanniemae.ezpie.data.connectors.DelimitedConnector;
import com.fanniemae.ezpie.data.connectors.DirectoryConnector;
import com.fanniemae.ezpie.data.connectors.ExcelConnector;
import com.fanniemae.ezpie.data.connectors.RestConnector;
import com.fanniemae.ezpie.data.connectors.SqlConnector;
import com.fanniemae.ezpie.data.transforms.DataTransform;
import com.fanniemae.ezpie.datafiles.DataReader;
import com.fanniemae.ezpie.datafiles.DataWriter;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.BinaryFileInfo;

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
	protected int _localCacheMinutes;
	
	protected boolean _localCacheEnabled;

	protected Element _dataSource;
	protected Element _connection;

	protected Map<Integer, Map<Integer, DataTransform>> _processingGroups = new HashMap<Integer, Map<Integer, DataTransform>>();

	public DataEngine(SessionManager session, boolean localCacheEnabled, int localCacheMinutes) {
		_session = session;
		_localCacheEnabled = localCacheEnabled;
		_localCacheMinutes = localCacheMinutes;
		_memoryLimit = _localCacheEnabled ? 0: _session.getMemoryLimit();
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
			throw new PieException(String.format("Staging directory %s does not exist.", path));

		_stagingPath = path.endsWith(File.separator) ? path : path + File.separator;
	}

	public DataStream getData(Element dataSource) {
		String finalDataFilename = FileUtilities.getDataFilename(_stagingPath, dataSource, _connection, _session.getTokenizer());

		//boolean dataSourceCacheable = StringUtilities.toBoolean(_session.optionalAttribute(dataSource, "DataCacheEnabled"), _session.cachingEnabled());
		DataStream dataStream = _localCacheEnabled ? checkCache(finalDataFilename) : null;
		if (dataStream != null) {
			Object expires = dataStream.getHeader().get(BinaryFileInfo.DateExpires);
			_session.addLogMessage("", "Cached Data", String.format("Using valid data cache file. The cache is set to expire %s", DateUtilities.toPrettyString((Date) expires)));
			return dataStream;
		}

		_dataSource = dataSource;
		defineProcessingGroups();
		List<String> tempFiles = new ArrayList<String>();
		try {
			String dataFilename = "";
			for (int iGroup = 0; iGroup < _processingGroupsCount; iGroup++) {
				dataFilename = (iGroup + 1 == _processingGroupsCount) ? finalDataFilename : FileUtilities.getRandomFilename(_stagingPath, "dat");
				Map<Integer, DataTransform> dataOperations = _processingGroups.get(iGroup);
				int operationCount = dataOperations.size();

				// Some data operations require access to then entire data stream
				// they cannot be combined with other operations. E.g. Sort, Join
				if ((operationCount == 1) && dataOperations.get(0).isolated()) {
					_session.addLogMessage("", String.format("Processing Group #%d of %d", iGroup + 1, _processingGroupsCount), "");
					dataOperations.get(0).addTransformLogMessage();
					dataStream = dataOperations.get(0).processDataStream(dataStream, _memoryLimit);
					_schema = dataStream.getSchema();
				} else {
					// These operations can be combined - multiple operations during one
					// pass through the data stream.
					try (DataConnector dc = getConnector(dataStream); DataWriter dw = new DataWriter(dataFilename, _memoryLimit, false)) {
						dc.open();
						String[][] schema = dc.getDataSourceSchema();
						if (operationCount > 0) {
							_session.addLogMessage("", String.format("Data Transform Group #%d of %d", iGroup + 1, _processingGroupsCount), "");
							// Update the schema based on the operations within this group.
							for (int i = 0; i < operationCount; i++) {
								dataOperations.get(i).addTransformLogMessage();
								schema = dataOperations.get(i).UpdateSchema(schema);
							}
						}
						_schema = schema;
						dw.setDataColumns(schema);
						long rowCount = 0;
						while (!dc.eof()) {
							Object[] dataRow = dc.getDataRow();
							if (dataRow == null) {
								continue;
							}
							if (operationCount > 0) {
								for (int i = 0; i < operationCount; i++) {
									dataRow = dataOperations.get(i).processDataRow(dataRow);
									if (dataRow == null) {
										break;
									}
								}
							}
							if (dataRow != null) {
								dw.writeDataRow(dataRow);
								rowCount++;
							}
						}
						Calendar calendarExpires = Calendar.getInstance();
						if (_localCacheEnabled) { //(_session.cachingEnabled())
							calendarExpires.add(Calendar.MINUTE, _localCacheMinutes); // _session.getCacheMinutes());
						}
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
				if (iGroup + 1 < _processingGroupsCount) {
					tempFiles.add(dataFilename);
				}
			}
			if (_session.cachingEnabled()) {
				dataStream = FileUtilities.writeDataStream(dataFilename, dataStream);
			}
		} catch (Exception ex) {
			throw ex;
		} finally {
			if (tempFiles.size() > 0) {
				int length = tempFiles.size();
				for (int i = 0; i < length; i++) {
					try {
						FileUtilities.deleteFile(tempFiles.get(i));
					} catch (Exception ex) {
						ExceptionUtilities.goSilent(ex);
						_session.addLogMessage(Constants.LOG_WARNING_MESSAGE, "Cleanup", String.format("Could not delete temporary file. Reason: %s", ex.getMessage()));
					}
				}
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
			throw new PieException("DataSource Type attribute not defined.");
		case "sp":
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
			throw new PieException(String.format("Requested DataSource Type=%s not currently supported.", connectorType));
		}
	}

	protected void defineProcessingGroups() {
		NodeList nlTransforms = XmlUtilities.selectNodes(_dataSource, "*");
		ExecutionPlanner planner = new ExecutionPlanner(_session);
		_processingGroups = planner.getExecutionPlan(nlTransforms);
		_processingGroupsCount = _processingGroups.size();
	}

	protected DataStream checkCache(String filename) {
		Boolean expired = false;
		String[][] schema = null;
		Map<BinaryFileInfo, Object> header = null;
		if (FileUtilities.isInvalidFile(filename))
			return null;

		try (DataReader dr = new DataReader(filename)) {
			schema = dr.getSchema();
			header = dr.getHeader();
			Date expires = dr.getBufferExpires();
			Date current = new Date();

			expired = expires.before(current);
			dr.close();
		} catch (Exception ex) {
			ExceptionUtilities.goSilent(ex);
			return null;
		}
		if (expired)
			return null;

		return new DataStream(filename, header, schema);
	}

}
