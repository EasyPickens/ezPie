package com.fanniemae.automation.data.transforms;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import org.w3c.dom.Element;

import com.fanniemae.automation.SessionManager;
import com.fanniemae.automation.common.DataStream;
import com.fanniemae.automation.common.FileUtilities;
import com.fanniemae.automation.common.StringUtilities;
import com.fanniemae.automation.datafiles.DataReader;
import com.fanniemae.automation.datafiles.DataWriter;
import com.fanniemae.automation.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Richard Monson
 * @since 2016-01-21
 * 
 */
public class Sort extends DataTransform {

	protected static final int SORT_ARRAY_MAX_ITEMS = 100000;

	protected DataStream _dataStream;

	protected int _numberOfKeys;
	protected String[] _columnNames;
	protected int[] _inputColumnIndexes;
	protected DataType[] _dataTypes;
	protected boolean[] _isAscending;

	protected ArrayList<String> _SortedFilenameBlocks = new ArrayList<String>();
	protected ArrayList<SortDataRow> _indexDataList = new ArrayList<SortDataRow>();
	protected SortDataRow[] _indexData;

	public Sort(SessionManager session, Element operation) {
		super(session, operation, false);

		String dataColumnList = _Session.getAttribute(operation, "DataColumns");
		String sortDirectionList = _Session.getAttribute(operation, "SortDirections");

		if (StringUtilities.isNullOrEmpty(dataColumnList)) {
			throw new RuntimeException("Sort transformation requires at least one column name in DataColumns.");
		}
		String[] columnNames = dataColumnList.split(",");
		String[] sortDirections = (StringUtilities.isNullOrEmpty(sortDirectionList)) ? null : sortDirectionList.split(",");

		_numberOfKeys = columnNames.length;
		_inputColumnIndexes = new int[_numberOfKeys];
		_isAscending = new boolean[_numberOfKeys];
		// Added a file position column so that sort is a "stable sort"
		// Stable sort = equal items remain in the original order.
		// Offset column name is ixStreamOffset
		_columnNames = new String[_numberOfKeys + 1];
		_columnNames[_numberOfKeys] = "ixStreamOffset";
		_dataTypes = new DataType[_numberOfKeys + 1];
		_dataTypes[_numberOfKeys] = DataType.LongData;

		if (sortDirections == null) {
			for (int i = 0; i < _numberOfKeys; i++) {
				_isAscending[i] = true;
				_columnNames[i] = columnNames[i];
			}
		} else {
			int length = Math.min(sortDirections.length, _numberOfKeys);
			for (int i = 0; i < length; i++) {
				_columnNames[i] = columnNames[i];
				String direction = sortDirections[i];
				if (StringUtilities.isNullOrEmpty(direction) || direction.toLowerCase().startsWith("asc")) {
					_isAscending[i] = true;
				} else {
					_isAscending[i] = false;
				}
			}
		}
	}

	@Override
	public boolean isTableLevel() {
		return true;
	}

	@Override
	public Object[] processDataRow(Object[] dataRow) {
		throw new RuntimeException("Sort requires access to the entire data set.  It cannot be combined with other data transformations.");
	}

	@Override
	public DataStream processDataStream(DataStream inputStream, int memoryLimit) {
		DataStream outputStream = null;

		try (DataReader dr = new DataReader(inputStream);) {
			String[] inputColumnNames = dr.getColumnNames();
			DataType[] inputColumnTypes = dr.getDataTypes();
			updateSortInstructions(inputColumnNames, inputColumnTypes);

			int indexCount = 0;
			while (!dr.eof()) {
				long offset = dr.getPosition();
				Object[] dataRow = dr.getDataRow();
				SortDataRow rowKeys = new SortDataRow(offset, _numberOfKeys);

				for (int i = 0; i < _numberOfKeys; i++) {
					Object dataPoint = _inputColumnIndexes[i] == -1 ? null : dataRow[_inputColumnIndexes[i]];
					DataType dataType = _inputColumnIndexes[i] == -1 ? DataType.StringData : inputColumnTypes[_inputColumnIndexes[i]];
					rowKeys.setDataPoint(i, dataPoint, dataType, _isAscending[i]);
				}
				_indexDataList.add(rowKeys);
				indexCount++;
				if (indexCount >= SORT_ARRAY_MAX_ITEMS) {
					// Sort the array
					_indexData = new SortDataRow[_indexDataList.size()]; 
		            _indexDataList.toArray(_indexData);
					_indexDataList.clear();
					Arrays.sort(_indexData);
					// Write the sorted data to an index file
					writeIndexFile();
					indexCount = 0;
				}
			}
			dr.close();
		} catch (Exception ex) {
			throw new RuntimeException(String.format("Error while running %s data stream transformation.", _TransformName), ex);
		}

		if (_indexDataList.size() > 0) {
			_indexData = new SortDataRow[_indexDataList.size()];
            _indexDataList.toArray(_indexData);
			_indexDataList.clear();
			//saveIndexToFile(_indexData, "C:\\Developers\\Code\\TestDirectory\\_Exports\\BeforeSort.txt");
			Arrays.sort(_indexData);
			//saveIndexToFile(_indexData, "C:\\Developers\\Code\\TestDirectory\\_Exports\\AfterSort.txt");
		}
		
		if (_SortedFilenameBlocks.size() > 0) {
			// Write final sorted index block
			writeIndexFile();
			// Combine sorted blocks to produce final sorted file.
		} else {
			// Sorted in memory, write sorted data file.
			outputStream = writeSortedFile(inputStream);
		}
		return outputStream;
	}

	protected void updateSortInstructions(String[] inputColumnNames, DataType[] inputColumnTypes) {
		ArrayList<String> inputNames = new ArrayList<String>(Arrays.asList(inputColumnNames));
		for (int i = 0; i < _numberOfKeys; i++) {
			_inputColumnIndexes[i] = inputNames.indexOf(_columnNames[i]);
			_dataTypes[i] = _inputColumnIndexes[i] == -1 ? DataType.StringData : inputColumnTypes[_inputColumnIndexes[i]];
		}
	}

	protected String writeIndexFile() {
		String blockFilename = FileUtilities.getRandomFilename(_Session.getStagingPath());
		try (DataWriter dw = new DataWriter(blockFilename);) {
			dw.setDataColumns(_columnNames, _dataTypes);
			for (SortDataRow keys : _indexDataList) {
				dw.writeDataRow(keys.getSortValues());
			}
			dw.close();
		} catch (IOException ex) {
			throw new RuntimeException("Could not write index data stream.", ex);
		}
		_SortedFilenameBlocks.add(blockFilename);
		_indexDataList = new ArrayList<SortDataRow>();
		return blockFilename;
	}

	protected DataStream writeSortedFile(DataStream inputStream) {
		DataStream outputStream = null;
		String sortedFilename = FileUtilities.getRandomFilename(_Session.getStagingPath());
		int rowCount = 0;
		try (DataReader dr = new DataReader(inputStream); DataWriter dw = new DataWriter(sortedFilename, 0)) {
			String[] columnNames = dr.getColumnNames();
			DataType[] columnTypes = dr.getDataTypes();
			dw.setDataColumns(columnNames, columnTypes);
			for (int i = 0; i < _indexData.length; i++) {
				long offset = _indexData[i].getRowStart();
				Object[] data = dr.getDataRowAt(offset);
				dw.writeDataRow(data);				
				rowCount++;
			}
//			for (SortDataRow keys : _indexData) {
////				dr.getDataRowAt(keys.getRowStart());
////				dw.writeDataRow(dr.getDataRow());
//				dw.writeDataRow(dr.getDataRowAt(keys.getRowStart()));				
//				rowCount++;
//			}
			Calendar calendarExpires = Calendar.getInstance();
			calendarExpires.add(Calendar.MINUTE,30);
			dw.setFullRowCount(rowCount);
			dw.setBufferFirstRow(1);
			dw.setBufferLastRow(rowCount);
			dw.setBufferExpires(calendarExpires.getTime());
			dw.setFullRowCountKnown(true);
			dw.close();
			dr.close();
			outputStream = dw.getDataStream();
			_indexDataList = null;
			_indexData = null;
		} catch (Exception ex) {
			throw new RuntimeException("Error while trying to write final sorted file.", ex);
		}
		return outputStream;
	}
	
	protected void saveIndexToFile(SortDataRow[] indexData, String outFilename) {
		try (FileWriter fw = new FileWriter(outFilename)) {
			for(int i=0; i < _columnNames.length;i++) {
				if (i > 0) fw.append(", ");
				fw.append(_columnNames[i]);
			}
			fw.append(_Session.getLineSeparator());
			for (int i = 0; i < indexData.length; i++) {
				//long offset = _indexData[i].getRowStart();
				fw.append(_indexData[i].getSortValuesAsCSV());
				fw.append(_Session.getLineSeparator());
			}
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
