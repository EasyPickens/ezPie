package com.fanniemae.automation.data.transforms;

import java.util.ArrayList;
import java.util.Arrays;

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
		ArrayList<SortDataRow> indexData = new ArrayList<SortDataRow>();

		try (DataReader br = new DataReader(inputStream);) {
			String[] inputColumnNames = br.getColumnNames();
			DataType[] inputColumnTypes = br.getDataTypes();
			updateSortArrays(inputColumnNames, inputColumnTypes);

			int indexCount = 0;
			while (!br.eof()) {
				Object[] dataRow = br.getDataRow();
				SortDataRow rowKeys = new SortDataRow(br.getPosition(), _numberOfKeys);

				for (int i = 0; i < _numberOfKeys; i++) {
					Object dataPoint = _inputColumnIndexes[i] == -1 ? null : dataRow[_inputColumnIndexes[i]];
					DataType dataType = _inputColumnIndexes[i] == -1 ? DataType.StringData : _dataTypes[_inputColumnIndexes[i]];
					rowKeys.setDataPoint(i, dataPoint, dataType, _isAscending[i]);
				}
				indexData.add(rowKeys);
				indexCount++;
				if (indexCount >= SORT_ARRAY_MAX_ITEMS) {
					// Sort the array and then save to file
					Arrays.sort(indexData.toArray());
					String blockFilename = FileUtilities.getRandomFilename(_Session.getStagingPath());
					try (DataWriter dw = new DataWriter(blockFilename);) {
						dw.setDataColumns(_columnNames, _dataTypes);
						for (SortDataRow keys : indexData) {
							dw.writeDataRow(keys.getSortValues());
						}
						dw.close();
					}
					// Start new block of data.
					_SortedFilenameBlocks.add(blockFilename);
					indexCount = 0;
					indexData = new ArrayList<SortDataRow>();
				}
			}
			if (indexData.size() > 0) {
				Arrays.sort(indexData.toArray());
			}
			br.close();
		} catch (Exception ex) {
			throw new RuntimeException(String.format("Error while running %s data stream transformation.", _TransformName), ex);
		}

		// **** have to write the final sorted file!!!!
		// Need to posiblly combine multiple sorted files.
		String sortedFilename = FileUtilities.getRandomFilename(_Session.getStagingPath());
		try (DataReader br = new DataReader(inputStream); DataWriter bw = new DataWriter(sortedFilename, memoryLimit)) {
			String[] aColumnNames = br.getColumnNames();
			DataType[] aDataTypes = br.getDataTypes();

			bw.setDataColumns(aColumnNames, aDataTypes);
			while (!br.eof()) {
				Object[] aDataRow = processDataRow(br.getDataRow());
				if (aDataRow != null) {
					bw.writeDataRow(aDataRow);
				}
			}

			bw.close();
			outputStream = bw.getDataStream();
		} catch (Exception ex) {
			throw new RuntimeException(String.format("Error while running %s data stream transformation.", _TransformName), ex);
		}
		return outputStream;
	}

	protected void updateSortArrays(String[] inputColumnNames, DataType[] inputColumnTypes) {
		ArrayList<String> inputNames = (ArrayList<String>) Arrays.asList(inputColumnNames);
		for (int i = 0; i < _numberOfKeys; i++) {
			_inputColumnIndexes[i] = inputNames.indexOf(_columnNames[i]);
			_dataTypes[i] = _inputColumnIndexes[i] == -1 ? DataType.StringData : inputColumnTypes[_inputColumnIndexes[i]];
		}

	}

}
