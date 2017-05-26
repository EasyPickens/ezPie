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

package com.fanniemae.ezpie.actions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.DataStream;
import com.fanniemae.ezpie.common.DateUtilities;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.XmlUtilities;
import com.fanniemae.ezpie.data.DataEngine;
import com.fanniemae.ezpie.datafiles.DataReader;
import com.fanniemae.ezpie.datafiles.DataWriter;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2015-12-21
 * 
 */

public class DataSet extends Action {

	protected List<DataStream> _dataStreams = new ArrayList<DataStream>();
	protected HashMap<String, String> _dataTokens = null;

	public DataSet(SessionManager session, Element action) {
		super(session, action);
	}

	@Override
	public String executeAction(HashMap<String, String> dataTokens) {
		_session.setDataTokens(dataTokens);
		DataEngine de = new DataEngine(_session);
		NodeList nl = XmlUtilities.selectNodes(_action, "DataSource");
		Node loopNode = XmlUtilities.selectSingleNode(_action, "Loop");

		int length = nl.getLength();
		if ((length == 0) && (loopNode == null)) {
			throw new RuntimeException("Each DataSet element requires at least one DataSource child element.");
		}

		// pull all the DataSource elements
		for (int i = 0; i < length; i++) {
			DataStream ds = de.getData((Element) (nl.item(i)));
			_dataStreams.add(ds);
		}

		// check for any loop elements
		if (loopNode != null) {
			_dataTokens = dataTokens;
			runLoopDataSources(loopNode);
		}

		if (length == 1) {
			// Only one data source (should be majority of the time)
			_session.addDataSet(_name, _dataStreams.get(0));
		} else if ((_dataStreams != null) && (_dataStreams.size() > 0)) {
			unionDataStreams();
		}
		_session.clearDataTokens();
		return null;
	}

	protected void unionDataStreams() {
		// Merge the schema column names into a final list.
		List<String> finalColumnNames = new ArrayList<String>();
		List<String> finalColumnTypes = new ArrayList<String>();
		List<String> usedColumnNames = new ArrayList<String>();
		int length = _dataStreams.size();
		for (int streamNumber = 0; streamNumber < length; streamNumber++) {
			String[][] schema = _dataStreams.get(streamNumber).getSchema();
			for (int columnNumber = 0; columnNumber < schema.length; columnNumber++) {
				String columnName = schema[columnNumber][0];
				String lowerName = columnName.toLowerCase();
				if (usedColumnNames.indexOf(lowerName) == -1) {
					finalColumnNames.add(columnName);
					finalColumnTypes.add(schema[columnNumber][1]);
					usedColumnNames.add(lowerName);
				}
			}
		}

		// Format the final schema for the datawriter
		int finalColumnCount = finalColumnNames.size();
		String[][] finalSchema = new String[finalColumnCount][2];
		for (int i = 0; i < finalColumnCount; i++) {
			finalSchema[i][0] = finalColumnNames.get(i);
			finalSchema[i][1] = finalColumnTypes.get(i);
		}

		// Union the data streams
		String dataFilename = FileUtilities.getDataFilename(_session.getStagingPath(), XmlUtilities.getOuterXml(_action), "***Multiple DataSources Union Together***");
		DataStream fullDataStream = null;
		_session.addLogMessage("", "Union Data", String.format("Union data into %s", dataFilename));
		try (DataWriter dw = new DataWriter(dataFilename, 0, false)) {
			dw.setDataColumns(finalSchema);
			long fullRowCount = 0;
			for (int streamNumber = 0; streamNumber < length; streamNumber++) {
				DataStream dataStream = _dataStreams.get(streamNumber);
				String[][] schema = dataStream.getSchema();
				if ((schema == null) || (schema.length == 0)) 
					continue;
				int[] columnIndexes = new int[schema.length];

				for (int columnNumber = 0; columnNumber < schema.length; columnNumber++) {
					String lowerName = schema[columnNumber][0].toLowerCase();
					columnIndexes[columnNumber] = usedColumnNames.indexOf(lowerName);
				}

				// Open this datastream and add the contents to the final file.
				try (DataReader dc = new DataReader(dataStream)) {
					long rowCount = 0;
					while (!dc.eof()) {
						Object[] aValues = dc.getDataRow();
						Object[] dataRow = new Object[finalColumnCount];
						for (int sourceColumnNumber = 0; sourceColumnNumber < aValues.length; sourceColumnNumber++) {
							dataRow[columnIndexes[sourceColumnNumber]] = aValues[sourceColumnNumber];
						}
						dw.writeDataRow(dataRow);
						rowCount++;
						fullRowCount++;
					}
					dc.close();
					_session.addLogMessage("", String.format("DataSource #%d", streamNumber + 1), String.format("%,d data rows added", rowCount));
				} catch (Exception e) {
					throw new RuntimeException("Error while writing the combined data file. ERROR: " + e.getMessage());
				}

			}
			Calendar calendarExpires = Calendar.getInstance();
			calendarExpires.add(Calendar.MINUTE, 30);
			dw.setFullRowCount(fullRowCount); // dc.getFullRowCount(_lFullRowCount));
			dw.setBufferFirstRow(1); // dc.getBufferFirstRow());
			dw.setBufferLastRow(fullRowCount); // dc.getBufferLastRow());
			dw.setBufferExpires(calendarExpires.getTime());
			dw.setFullRowCountKnown(true); // dc.getFullRowCountKnown());
			dw.close();

			fullDataStream = dw.getDataStream();
			_session.addLogMessage("", "Data Returned", String.format("%,d rows (%,d bytes in %s)", fullRowCount, fullDataStream.getSize(), fullDataStream.IsMemory() ? "memorystream" : "filestream"));
			_session.addDataSet(_name, fullDataStream);
		} catch (RuntimeException ex) {
			throw ex;
		} catch (IOException e) {
			throw new RuntimeException("Error while writing the combined data file. " + e.getMessage());
		}
	}

	protected void runLoopDataSources(Node loopNode) {
		String dataSetName = requiredAttribute(loopNode, "DataSetName");
		DataStream _dataStream = _session.getDataStream(dataSetName);
		int rowNumber = 0;
		try (DataReader dr = new DataReader(_dataStream)) {
			String[] colNames = dr.getColumnNames();
			DataType[] dataTypes = dr.getDataTypes();
			while (!dr.eof()) {
				Object[] dataRow = dr.getDataRow();

				HashMap<String, String> currentDataTokens = new HashMap<String, String>();
				// Combine the tokens provided via the parameter with this row.
				if (_dataTokens != null) {
					currentDataTokens.putAll(_dataTokens);
				}

				// Load values into dataTokens
				for (int i = 0; i < colNames.length; i++) {
					if (dataRow[i] == null) {
						currentDataTokens.put(colNames[i], "");
					} else if (dataTypes[i] == DataType.DateData) {
						currentDataTokens.put(colNames[i], DateUtilities.toIsoString((Date) dataRow[i]));
					} else if (dataTypes[i] == DataType.StringData) {
						currentDataTokens.put(colNames[i], (String) dataRow[i]);
					} else {
						currentDataTokens.put(colNames[i], dataRow[i].toString());
					}
				}

				_session.setDataTokens(currentDataTokens);
				NodeList nl = XmlUtilities.selectNodes(loopNode, "DataSource");
				int length = nl.getLength();
				// pull all the DataSource elements
				DataEngine de = new DataEngine(_session);
				for (int i = 0; i < length; i++) {
					Element dataSource = (Element) (nl.item(i));
					// adding a random GUID to ensure that each datasource gets a unique file name.
					dataSource.setAttribute("LoopControlMarker", UUID.randomUUID().toString());
					_dataStreams.add(de.getData(dataSource));
				}
				_session.clearDataTokens();
			}
			dr.close();
			// _session.addLogMessage("", "Data", String.format("%,d rows of data written.", iRowCount));
			// _session.addLogMessage("", "Completed", String.format("Data saved to %s", _outputFilename));
		} catch (Exception e) {
			RuntimeException ex = new RuntimeException(String.format("Error while looping through the data (row %,d)", rowNumber), e);
			throw ex;
		}
	}

}
