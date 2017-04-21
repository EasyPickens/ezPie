/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPIE/wiki/Fannie-Mae-Open-Source-Licensing-Project
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.data.transforms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.DataStream;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.common.XmlUtilities;
import com.fanniemae.ezpie.data.DataEngine;
import com.fanniemae.ezpie.datafiles.DataReader;
import com.fanniemae.ezpie.datafiles.DataWriter;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-02-04
 * 
*/

public class Join extends DataTransform {
	protected int INDEX_BUFFER_LIMIT = 10000;

	protected JoinType _joinType;
	protected String _joinText;

	protected Node _rightDataSource;

	protected int _indexColumnCount;

	protected String[] _finalColumnNames;
	protected DataType[] _finalDataTypes;

	protected JoinSchemaColumnEntry[] _joinSchema;

	protected String[] _leftJoinColumns;
	protected String[] _rightJoinColumns;
	protected String[] _leftIndexNames;
	protected String[] _rightIndexNames;
	protected String[] _leftColumnNames;
	protected String[] _rightColumnNames;

	protected DataType[] _leftIndexTypes;
	protected DataType[] _rightIndexTypes;
	protected DataType[] _leftColumnTypes;
	protected DataType[] _rightColumnTypes;

	protected IndexDataRow[] _leftIndexBuffer;
	protected IndexDataRow[] _rightIndexBuffer;

	protected enum JoinType {
		INNERJOIN, LEFTOUTERJOIN, RIGHTOUTERJOIN, OUTERJOIN, UNION, CROSSJOIN
	}

	protected enum JoinRules {
		LEFTONLY, RIGHTONLY, BOTHSIDES
	}

	public Join(SessionManager session, Element operation) {
		super(session, operation, false);

		String leftColumnList = _session.getAttribute(operation, "LeftColumns");
		String rightColumnList = _session.getAttribute(operation, "RightColumns");

		if (StringUtilities.isNullOrEmpty(leftColumnList)) {
			throw new RuntimeException("Join requires at least one column name in LeftColumns.");
		} else if (StringUtilities.isNullOrEmpty(rightColumnList)) {
			throw new RuntimeException("Join requires at least one column name in RightColumns.");
		}

		_leftJoinColumns = StringUtilities.split(leftColumnList);
		_rightJoinColumns = StringUtilities.split(rightColumnList);

		if (_leftJoinColumns.length != _rightJoinColumns.length) {
			throw new RuntimeException(String.format("The number of columns for the left and right data sets must be equal (%d left columns != %d right columns)", _leftJoinColumns.length, _rightJoinColumns.length));
		}

		String joinType = _session.getAttribute(operation, "JoinType");
		if (StringUtilities.isNullOrEmpty(joinType)) {
			joinType = "Inner";
		}

		_joinType = setJoinType(joinType);
		_indexColumnCount = _leftJoinColumns.length;

		StringBuilder sb = new StringBuilder(_joinText);
		for (int i = 0; i < _indexColumnCount; i++) {
			if (i > 0)
				sb.append(",");
			sb.append(String.format(" left.%s = right.%s", _leftJoinColumns[i], _rightJoinColumns[i]));
		}
		_transformInfo.append(sb.toString());

		_rightDataSource = XmlUtilities.selectSingleNode(operation, "DataSource");
		if (_rightDataSource == null) {
			throw new RuntimeException("Missing right side data source.  Joins require a nested DataSource for the right side.");
		}
	}

	@Override
	public boolean isTableLevel() {
		return true;
	}

	@Override
	public Object[] processDataRow(Object[] dataRow) {
		throw new RuntimeException("Join requires access to the entire data set.  It cannot be combined with other data transformations.");
	}

	@Override
	public DataStream processDataStream(DataStream inputStream, int memoryLimit) {
		if (_joinType == JoinType.UNION) {
			return unionDataStreams(inputStream, memoryLimit);
		} 
		return joinDataStreams(inputStream, memoryLimit);
	}

	protected JoinType setJoinType(String join) {
		switch (join.toLowerCase()) {
		case "inner":
			_joinText = "Inner Join";
			return JoinType.INNERJOIN;
		case "left":
		case "left outer":
			_joinText = "Left Outer";
			return JoinType.LEFTOUTERJOIN;
		case "right":
		case "right outer":
			_joinText = "Right Outer";
			return JoinType.RIGHTOUTERJOIN;
		case "outer":
		case "full outer":
			_joinText = "Full Outer";
			return JoinType.OUTERJOIN;
		case "union":
			_joinText = "Union";
			return JoinType.UNION;
		case "cross":
		case "cross join":
			// May not add support for this type of join until requested.  Very edge case and could create HUGE datasets.			
			_joinText = "Cross Join";
			return JoinType.CROSSJOIN;
		default:
			throw new RuntimeException(String.format("%s join type is not currently supported.", join));
		}
	}

	protected void mergeSchemas() {
		List<JoinSchemaColumnEntry> schema = new ArrayList<JoinSchemaColumnEntry>();
		Map<String, Boolean> usedColumnNames = new HashMap<String, Boolean>();
		// populate the left side information
		for (int i = 0; i < _leftColumnNames.length; i++) {
			String name = _leftColumnNames[i];
			if (!usedColumnNames.containsKey(name.toLowerCase())) {
				schema.add(new JoinSchemaColumnEntry(name, _leftColumnTypes[i], i));
				usedColumnNames.put(name.toLowerCase(), true);
			}
		}
		// populate the right side information
		for (int i = 0; i < _rightColumnNames.length; i++) {
			String name = _rightColumnNames[i];
			if (!usedColumnNames.containsKey(name.toLowerCase())) {
				schema.add(new JoinSchemaColumnEntry(name, _rightColumnTypes[i], true, i));
				usedColumnNames.put(name.toLowerCase(), true);
			}
		}
		_joinSchema = new JoinSchemaColumnEntry[schema.size()];
		schema.toArray(_joinSchema);

		_finalColumnNames = new String[schema.size()];
		_finalDataTypes = new DataType[schema.size()];
		for (int i = 0; i < _joinSchema.length; i++) {
			_finalColumnNames[i] = _joinSchema[i].getColumnName();
			_finalDataTypes[i] = _joinSchema[i].getColumnType();
		}
	}

	protected void bufferIndexData(DataReader dr, boolean isRight) {
		List<IndexDataRow> buffer = new ArrayList<IndexDataRow>();
		try {
			int rowsBuffered = 0;
			while (!dr.eof()) {
				Object[] indexRow = dr.getDataRow();
				IndexDataRow rowKeys = new IndexDataRow(1, (long) indexRow[_indexColumnCount], _indexColumnCount);
				for (int i = 0; i < _indexColumnCount; i++) {
					Object dataPoint = indexRow[i];
					DataType dataType = isRight ? _rightIndexTypes[i] : _leftIndexTypes[i];
					rowKeys.setDataPoint(i, dataPoint, dataType, true);
				}
				buffer.add(rowKeys);
				rowsBuffered++;
				if (rowsBuffered >= INDEX_BUFFER_LIMIT) {
					break;
				}
			}
			if (isRight) {
				_rightIndexBuffer = new IndexDataRow[buffer.size()];
				buffer.toArray(_rightIndexBuffer);
			} else {
				_leftIndexBuffer = new IndexDataRow[buffer.size()];
				buffer.toArray(_leftIndexBuffer);
			}
		} catch (IOException e) {
			throw new RuntimeException("Error while buffering join index data.", e);
		}
	}
	
	protected DataStream joinDataStreams(DataStream inputStream, int memoryLimit) {
		DataStream outputStream = null;
		// Get the right side data (create new instance of data engine)
		DataEngine de = new DataEngine(_session);
		DataStream rightDataStream = de.getData((Element) _rightDataSource);
		// Index the right and left dataStreams on the join columns
		_session.addLogMessage("", "Index Left", "Indexing the left side data.");
		DataTransform indexData = TransformFactory.getIndexTransform(_session, _leftJoinColumns);
		DataStream leftIndexStream = indexData.processDataStream(inputStream, memoryLimit);
		_session.addLogMessage("", "Index Right", "Indexing the right side data.");
		indexData = TransformFactory.getIndexTransform(_session, _rightJoinColumns);
		DataStream rightIndexStream = indexData.processDataStream(rightDataStream, memoryLimit);
		// Join the data and write the final file.
		String outputFilename = FileUtilities.getRandomFilename(_session.getStagingPath(), "dat");
		//@formatter:off
		try (DataReader leftIndex = new DataReader(leftIndexStream); 
				DataReader leftData = new DataReader(inputStream); 
				DataReader rightIndex = new DataReader(rightIndexStream); 
				DataReader rightData = new DataReader(rightDataStream);
				DataWriter dw = new DataWriter(outputFilename, memoryLimit);) {
            //@formatter:on
			_leftIndexNames = leftIndex.getColumnNames();
			_leftIndexTypes = leftIndex.getDataTypes();
			_leftColumnNames = leftData.getColumnNames();
			_leftColumnTypes = leftData.getDataTypes();
			_rightIndexNames = rightIndex.getColumnNames();
			_rightIndexTypes = rightIndex.getDataTypes();
			_rightColumnNames = rightData.getColumnNames();
			_rightColumnTypes = rightData.getDataTypes();
			// Merge the two schemas (remove duplicate columns from the right side)
			mergeSchemas();
			dw.setDataColumns(_finalColumnNames, _finalDataTypes);
			Object[] completeDataRow = new Object[_joinSchema.length];
			int rowCount = 0;
			int rightLastIndexRead = 0;
			// Need to adjust code to work with large data sets.
			while (!leftIndex.eof() && !rightIndex.eof()) {
				bufferIndexData(leftIndex, false);
				bufferIndexData(rightIndex, true);
				int rightStartIndex = 0;
				for (int left = 0; left < _leftIndexBuffer.length; left++) {
					Object[] leftRow = leftData.getDataRowAt(_leftIndexBuffer[left].getRowStart());
					Arrays.fill(completeDataRow, null);
					for (int i = 0; i < leftRow.length; i++) {
						completeDataRow[i] = leftRow[i];
					}
					boolean writtenRow = false;
					boolean firstMatch = false;
					for (int right = rightStartIndex; right < _rightIndexBuffer.length; right++) {
						int compareValue = _leftIndexBuffer[left].compareValues(_rightIndexBuffer[right]);
						rightLastIndexRead = right;
						// 0 ==> values match
						// -n ==> right side comes after the left (right is greater than)
						// n ==> right side comes before the left (right is less than)
						if (compareValue == 0) {
							_rightIndexBuffer[right].setUsedFlag(true);
							if (!firstMatch) {
								rightStartIndex = right;
								firstMatch = true;
							}
							Object[] rightRow = rightData.getDataRowAt(_rightIndexBuffer[right].getRowStart());
							for (int i = 0; i < _joinSchema.length; i++) {
								if (_joinSchema[i].isRightSide()) {
									completeDataRow[i] = rightRow[_joinSchema[i].getColumnIndex()];
								}
							}
							dw.writeDataRow(completeDataRow);
							rowCount++;
							writtenRow = true;
						} else if ((compareValue > 0) && !_rightIndexBuffer[right].haveUsed() && ((_joinType == JoinType.RIGHTOUTERJOIN) || (_joinType == JoinType.OUTERJOIN))) {
							Object[] rightSideOnlyDataRow = new Object[_joinSchema.length];
							Object[] rightRow = rightData.getDataRowAt(_rightIndexBuffer[right].getRowStart());
							for (int i = 0; i < _joinSchema.length; i++) {
								if (_joinSchema[i].isRightSide()) {
									rightSideOnlyDataRow[i] = rightRow[i];
								}
							}
							dw.writeDataRow(rightSideOnlyDataRow);
							rowCount++;
							writtenRow = true;
						} else if (compareValue < 0) {
							break;
						}
					}
					if (!writtenRow && ((_joinType == JoinType.LEFTOUTERJOIN) || (_joinType == JoinType.OUTERJOIN))) {
						dw.writeDataRow(completeDataRow);
						rowCount++;
					}
				}
			}
			if (leftIndex.eof() && (rightLastIndexRead + 1 < _rightIndexBuffer.length) && ((_joinType == JoinType.OUTERJOIN) || (_joinType == JoinType.RIGHTOUTERJOIN))) {
				for (int right = rightLastIndexRead + 1; right < _rightIndexBuffer.length; right++) {
					Object[] rightSideOnlyDataRow = new Object[_joinSchema.length];
					Object[] rightRow = rightData.getDataRowAt(_rightIndexBuffer[right].getRowStart());
					for (int i = 0; i < _joinSchema.length; i++) {
						if (_joinSchema[i].isRightSide()) {
							rightSideOnlyDataRow[i] = rightRow[i];
						}
					}
					dw.writeDataRow(rightSideOnlyDataRow);
					rowCount++;
				}
			}
			rightData.close();
			rightIndex.close();
			leftData.close();
			leftIndex.close();

			Calendar calendarExpires = Calendar.getInstance();
			calendarExpires.add(Calendar.MINUTE, 30);
			dw.setFullRowCount(rowCount);
			dw.setBufferFirstRow(1);
			dw.setBufferLastRow(rowCount);
			dw.setBufferExpires(calendarExpires.getTime());
			dw.setFullRowCountKnown(true);
			dw.close();
			outputStream = dw.getDataStream();
		} catch (Exception ex) {
			throw new RuntimeException("Error while joining data sources.", ex);
		}
		return outputStream;
	}
	
	protected DataStream unionDataStreams(DataStream inputStream, int memoryLimit) {
		DataStream outputStream = null;
		// Get the right side data (create new instance of data engine)
		DataEngine de = new DataEngine(_session);
		DataStream rightDataStream = de.getData((Element) _rightDataSource);
		// Union the data and write the final file.
		String outputFilename = FileUtilities.getRandomFilename(_session.getStagingPath(), "dat");
		//@formatter:off
		try (DataReader leftData = new DataReader(inputStream);
				DataReader rightData = new DataReader(rightDataStream);
				DataWriter dw = new DataWriter(outputFilename, memoryLimit);) {
            //@formatter:on
			_leftColumnNames = leftData.getColumnNames();
			_leftColumnTypes = leftData.getDataTypes();
			_rightColumnNames = rightData.getColumnNames();
			_rightColumnTypes = rightData.getDataTypes();
			// Merge the two schemas (remove duplicate columns from the right side)
			mergeSchemas();
			dw.setDataColumns(_finalColumnNames, _finalDataTypes);
			Object[] completeDataRow = new Object[_joinSchema.length];
			int rowCount = 0;
			
			// Write left side data first
			while (!leftData.eof()) {
				Object[] leftRow = leftData.getDataRow();
				Arrays.fill(completeDataRow, null);
				for (int i = 0; i < leftRow.length; i++) {
					completeDataRow[i] = leftRow[i];
				}
				dw.writeDataRow(completeDataRow);
				rowCount++;
			}
			
			// Write right side data next
			while (!rightData.eof()) {
				Object[] rightRow = rightData.getDataRow();
				Arrays.fill(completeDataRow, null);
				for (int i = 0; i < _joinSchema.length; i++) {
					if (_joinSchema[i].isRightSide()) {
						completeDataRow[i] = rightRow[i];
					}
				}
				dw.writeDataRow(completeDataRow);
				rowCount++;
			}
			rightData.close();
			leftData.close();

			Calendar calendarExpires = Calendar.getInstance();
			calendarExpires.add(Calendar.MINUTE, 30);
			dw.setFullRowCount(rowCount);
			dw.setBufferFirstRow(1);
			dw.setBufferLastRow(rowCount);
			dw.setBufferExpires(calendarExpires.getTime());
			dw.setFullRowCountKnown(true);
			dw.close();
			outputStream = dw.getDataStream();
		} catch (Exception ex) {
			throw new RuntimeException("Error during union operation of the data sources.", ex);
		}
		return outputStream;		
	}
}
