/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.data.transforms;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.Constants;
import com.fanniemae.ezpie.common.DataStream;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.datafiles.DataReader;
import com.fanniemae.ezpie.datafiles.DataWriter;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2016-01-21
 * 
*/

public class Index extends DataTransform {

	protected static final int INDEX_ARRAY_MAX_ITEMS = 100000;

	protected DataStream _dataStream;

	protected int _numberOfKeys;

	protected String[] _columnNames;
	protected int[] _inputColumnIndexes;
	protected DataType[] _dataTypes;
	protected boolean[] _isAscending;
	protected DataReader[] _indexStreams;
	protected int[] _bufferedRows;

	protected int _countIndexFiles;
	protected int _activeBuffers;
	protected int _bufferSize;

	protected ArrayList<String> _indexFilenameBlocks = new ArrayList<String>();
	protected ArrayList<IndexDataRow> _indexDataList = new ArrayList<IndexDataRow>();
	protected IndexDataRow[] _indexData;

	public Index(SessionManager session, String[] columnNames) {
		super(session, "Index");
		initialize(columnNames,null);
	}
	
	public Index(SessionManager session, Element transform) {
		super(session, transform, false);
		_isolate = true;

		String dataColumnList = _session.getAttribute(transform, "DataColumns");
		String indexDirectionList = _session.getAttribute(transform, "SortDirections");

		if (StringUtilities.isNullOrEmpty(dataColumnList)) {
			throw new PieException(String.format("%s transform requires at least one column name in DataColumns.",transform.getNodeName()));
		}

		String[] columnNames = dataColumnList.split(",");
		String[] indexDirections = (StringUtilities.isNullOrEmpty(indexDirectionList)) ? null : indexDirectionList.split(",");
		
		initialize(columnNames, indexDirections);
	}

	@Override
	public Object[] processDataRow(Object[] dataRow) {
		throw new PieException(String.format("%s requires access to the entire data set.  It cannot be combined with other data transformations.",_transform.getNodeName()));
	}

	@Override
	public DataStream processDataStream(DataStream inputStream, int memoryLimit) {
		DataStream outputStream = null;

		try (DataReader dr = new DataReader(inputStream);) {
			String[] inputColumnNames = dr.getColumnNames();
			DataType[] inputColumnTypes = dr.getDataTypes();
			updateIndexInstructions(inputColumnNames, inputColumnTypes);

			int indexEntryCount = 0;
			while (!dr.eof()) {
				long offset = dr.getPosition();
				Object[] dataRow = dr.getDataRow();
				IndexDataRow rowKeys = new IndexDataRow(offset, _numberOfKeys);

				for (int i = 0; i < _numberOfKeys; i++) {
					Object dataPoint = _inputColumnIndexes[i] == -1 ? null : dataRow[_inputColumnIndexes[i]];
					DataType dataType = _inputColumnIndexes[i] == -1 ? DataType.StringData : inputColumnTypes[_inputColumnIndexes[i]];
					rowKeys.setDataPoint(i, dataPoint, dataType, _isAscending[i]);
				}
				_indexDataList.add(rowKeys);
				indexEntryCount++;
				if (indexEntryCount >= INDEX_ARRAY_MAX_ITEMS) {
					// Sort the array
					_indexData = new IndexDataRow[_indexDataList.size()];
					_indexDataList.toArray(_indexData);
					_indexDataList.clear();
					Arrays.sort(_indexData);
					// Write the sorted data to an index file, start next block of data
					writeIndexFile();
					indexEntryCount = 0;
				}
			}
			dr.close();
		} catch (Exception ex) {
			throw new PieException(String.format("Error while running %s data stream transformation.", _transformElementName), ex);
		}

		if (_indexDataList.size() > 0) {
			_indexData = new IndexDataRow[_indexDataList.size()];
			_indexDataList.toArray(_indexData);
			_indexDataList.clear();
			// saveIndexToTextFile(_indexData, "C:\\Developers\\Code\\TestDirectory\\_Exports\\BeforeSort.txt");
			Arrays.sort(_indexData);
			// saveIndexToTextFile(_indexData, "C:\\Developers\\Code\\TestDirectory\\_Exports\\AfterSort.txt");
		}

		if (_indexFilenameBlocks.size() > 0) {
			// Write final index block
			writeIndexFile();
			// Combine index blocks to produce final sorted file.
			outputStream = mergeExternalIndexFiles();
		} else {
			// Sorted in memory, create indexStream (could be memory or file).
			outputStream = writeIndexDataStream(true);
		}
		return outputStream;
	}
	
	protected void initialize(String[] columnNames, String[] directions) {
		_numberOfKeys = columnNames.length;
		_inputColumnIndexes = new int[_numberOfKeys];
		_isAscending = new boolean[_numberOfKeys];

		// Using the file offset to ensure that the sort is "stable".
		// Offset column name is ixStreamOffset
		_columnNames = new String[_numberOfKeys + 1];
		_columnNames[_numberOfKeys] = "ixStreamOffset";
		_dataTypes = new DataType[_numberOfKeys + 1];
		_dataTypes[_numberOfKeys] = DataType.LongData;

		if (directions == null) {
			for (int i = 0; i < _numberOfKeys; i++) {
				_isAscending[i] = true;
				_columnNames[i] = columnNames[i];
			}
		} else {
			int length = Math.min(directions.length, _numberOfKeys);
			for (int i = 0; i < length; i++) {
				_columnNames[i] = columnNames[i].trim();
				String direction = directions[i];
				if (StringUtilities.isNullOrEmpty(direction) || direction.toLowerCase().trim().startsWith("asc")) {
					_isAscending[i] = true;
				} else {
					_isAscending[i] = false;
				}
			}
		}

		// Added to log the instructions defined:
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < _numberOfKeys; i++) {
			if (i > 0)
				sb.append(", ");
			sb.append(_columnNames[i]);
			sb.append(_isAscending[i] ? " ASC" : " DESC");
		}
		_transformInfo.appendFormat("Order by: %s", sb.toString());
	}

	protected void updateIndexInstructions(String[] inputColumnNames, DataType[] inputColumnTypes) {
		ArrayList<String> inputNames = new ArrayList<String>(Arrays.asList(inputColumnNames));
		for (int i = 0; i < _numberOfKeys; i++) {
			_inputColumnIndexes[i] = inputNames.indexOf(_columnNames[i]);
			_dataTypes[i] = _inputColumnIndexes[i] == -1 ? DataType.StringData : inputColumnTypes[_inputColumnIndexes[i]];
		}
	}

	protected String writeIndexFile() {
		DataStream indexStream = writeIndexDataStream(false);
		_indexFilenameBlocks.add(indexStream.getFilename());
		_indexDataList = new ArrayList<IndexDataRow>();
		return indexStream.getFilename();
	}

	protected DataStream writeIndexDataStream(boolean useMemorySettings) {
		DataStream indexStream = null;

		int memoryLimit = useMemorySettings ? _session.getMemoryLimit() : 0;

		String indexFilename = FileUtilities.getRandomFilename(_session.getStagingPath(), "ntx");
		try (DataWriter dw = new DataWriter(indexFilename, memoryLimit);) {
			dw.setDataColumns(_columnNames, _dataTypes);
			for (IndexDataRow keys : _indexData) {
				dw.writeDataRow(keys.getIndexValues());
			}
			dw.close();
			indexStream = dw.getDataStream();
		} catch (IOException ex) {
			throw new RuntimeException("Could not write index data stream.", ex);
		}
		return indexStream;
	}

//	protected DataStream writeSortedFile(DataStream inputStream) {
//		DataStream outputStream = null;
//		String sortedFilename = FileUtilities.getRandomFilename(_Session.getStagingPath());
//		int rowCount = 0;
//		try (DataReader dr = new DataReader(inputStream); DataWriter dw = new DataWriter(sortedFilename, 0)) {
//			String[] columnNames = dr.getColumnNames();
//			DataType[] columnTypes = dr.getDataTypes();
//			dw.setDataColumns(columnNames, columnTypes);
//			for (IndexDataRow keys : _indexData) {
//				dw.writeDataRow(dr.getDataRowAt(keys.getRowStart()));
//				rowCount++;
//			}
//			Calendar calendarExpires = Calendar.getInstance();
//			calendarExpires.add(Calendar.MINUTE, 30);
//			dw.setFullRowCount(rowCount);
//			dw.setBufferFirstRow(1);
//			dw.setBufferLastRow(rowCount);
//			dw.setBufferExpires(calendarExpires.getTime());
//			dw.setFullRowCountKnown(true);
//			dw.close();
//			dr.close();
//			outputStream = dw.getDataStream();
//			_indexDataList = null;
//			_indexData = null;
//			_Session.addLogMessage("", "Data Returned", String.format("%,d rows (%,d bytes in %s)", rowCount, outputStream.getSize(), outputStream.IsMemory() ? "memorystream" : "filestream"));
//		} catch (Exception ex) {
//			throw new RuntimeException("Error while trying to write final index file.", ex);
//		}
//		return outputStream;
//	}

	// This method is only used for detailed development debugging - It is not used by application. If the method is needed, 
	// I will add a static version to the ArrayUtilities class in the common package.  
	protected void saveIndexToTextFile(IndexDataRow[] indexData, String outFilename) {
		try (FileWriter fw = new FileWriter(outFilename)) {
			for (int i = 0; i < _columnNames.length; i++) {
				if (i > 0)
					fw.append(", ");
				fw.append(_columnNames[i]);
			}
			fw.append(_session.getLineSeparator());
			for (int i = 0; i < indexData.length; i++) {
				fw.append(_indexData[i].getIndexValuesAsCSV());
				fw.append(_session.getLineSeparator());
			}
			fw.close();
		} catch (IOException e) {
			throw new RuntimeException("Error while trying to save index file.", e);
		}
	}

	// Reads the external index files to create the merged index file.
	protected DataStream mergeExternalIndexFiles() {
		DataStream outputStream = null;
		String indexFilename = FileUtilities.getRandomFilename(_session.getStagingPath(), "ntx");

		// 1. Evenly divide the max items among the streams.
		// 2. read that many from each stream.
		// 3. update a count of items from each stream.

		// 4. sort the array.
		// 5. write the output - count down the items from each stream
		// 6. if count reaches 0 for a stream, read the next n rows from the index & update the count
		// 7. repeat 4-7 until every stream is empty.

		_countIndexFiles = _indexFilenameBlocks.size();
		_activeBuffers = _countIndexFiles;
		_bufferSize = INDEX_ARRAY_MAX_ITEMS / _activeBuffers;
		_indexStreams = new DataReader[_countIndexFiles];
		_bufferedRows = new int[_countIndexFiles];

		try (DataWriter dw = new DataWriter(indexFilename, 0)) {
			String[] columnNames = null;
			DataType[] columnTypes = null;
			// Open the index files and start reading data.
			for (int i = 0; i < _countIndexFiles; i++) {
				_indexStreams[i] = new DataReader(_indexFilenameBlocks.get(i));
				_bufferedRows[i] = bufferIndexData(i);
				if (i == 0) {
					columnNames = _indexStreams[0].getColumnNames();
					columnTypes = _indexStreams[0].getDataTypes();
				}
			}
			dw.setDataColumns(columnNames, columnTypes);

			_indexData = new IndexDataRow[_indexDataList.size()];
			_indexDataList.toArray(_indexData);
			_indexDataList.clear();
			Arrays.sort(_indexData);

			int rowCount = 0;
			int streamChannel = 0;
			int nextRow = 0;
			while ((_activeBuffers > 0) && (_indexData.length > 0)) {
				int length = _indexData.length;
				for (int i = 0; i < length; i++) {
					streamChannel = _indexData[i].getStreamChannel();
					dw.writeDataRow(_indexData[i].getIndexValues());
					rowCount++;
					_bufferedRows[streamChannel]--;
					if (_bufferedRows[streamChannel] == 0) {
						nextRow = i + 1;
						break;
					}
				}
				if (_bufferedRows[streamChannel] == 0) {
					// Copy the remaining elements to a new arraylist - before adding
					// additional index rows.
					_indexDataList = new ArrayList<IndexDataRow>();
					for (int i = nextRow; i < length; i++) {
						_indexDataList.add(_indexData[i]);
					}
					// Reload buffer from that stream
					_bufferedRows[streamChannel] = bufferIndexData(streamChannel);
					if (_bufferedRows[streamChannel] == -1) {
						_activeBuffers--;
						_bufferSize = INDEX_ARRAY_MAX_ITEMS / Math.max(_activeBuffers, 1);
					}
					// Sort the array
					_indexData = new IndexDataRow[_indexDataList.size()];
					_indexDataList.toArray(_indexData);
					_indexDataList.clear();
					Arrays.sort(_indexData);
				}
			}

			Calendar calendarExpires = Calendar.getInstance();
			calendarExpires.add(Calendar.MINUTE, 30);
			dw.setFullRowCount(rowCount);
			dw.setBufferFirstRow(1);
			dw.setBufferLastRow(rowCount);
			dw.setBufferExpires(calendarExpires.getTime());
			dw.setFullRowCountKnown(true);
			dw.close();
			outputStream = dw.getDataStream();
			_indexDataList = null;
			_indexData = null;
			_session.addLogMessage("", "Data Returned", String.format("%,d rows (%,d bytes in %s)", rowCount, outputStream.getSize(), outputStream.IsMemory() ? "memorystream" : "filestream"));
		} catch (Exception ex) {
			throw new RuntimeException("Could not combine external index streams.", ex);
		} finally {
			// Be sure to close every input stream & delete the temporary index files.
			for (int i = 0; i < _countIndexFiles; i++) {
				if (_indexStreams[i] != null) {
					try {
						_indexStreams[i].close();
						FileUtilities.deleteFile(_indexStreams[i].getFilename());
					} catch (Exception ex) {
						String message = ((ex != null) && (ex.getMessage() != null)) ? ex.getMessage() : "No Details";
						_session.addLogMessage(Constants.LOG_WARNING_MESSAGE, "Stream", "Tried to close stream: " + message);
					}
				}
			}
		}
		return outputStream;
	}

	protected int bufferIndexData(int streamChannel) {
		int iCount = 0;
		try {
			if ((_bufferedRows[streamChannel] == -1) || _indexStreams[streamChannel].eof()) {
				return -1;
			}

			while (!_indexStreams[streamChannel].eof() && iCount < _bufferSize) {
				Object[] indexRow = _indexStreams[streamChannel].getDataRow();
				IndexDataRow rowKeys = new IndexDataRow(streamChannel, (long) indexRow[_numberOfKeys], _numberOfKeys);
				for (int i = 0; i < _numberOfKeys; i++) {
					Object dataPoint = indexRow[i];
					DataType dataType = _dataTypes[i];
					rowKeys.setDataPoint(i, dataPoint, dataType, _isAscending[i]);
				}
				_indexDataList.add(rowKeys);
				iCount++;
			}
		} catch (IOException e) {
			throw new RuntimeException("Error while trying to read the index file.", e);
		}
		return iCount;
	}
}
