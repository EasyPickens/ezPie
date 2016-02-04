package com.fanniemae.automation.data.transforms;

import org.w3c.dom.Element;

import com.fanniemae.automation.SessionManager;
import com.fanniemae.automation.common.DataStream;
import com.fanniemae.automation.common.StringUtilities;

public class Join extends DataTransform {
	
	protected int _columnCount;
	
	protected String[] _leftColumnNames;
	protected String[] _rightColumnNames;

	public Join(SessionManager session, Element operation) {
		super(session, operation, false);

		String leftColumnList = _Session.getAttribute(operation, "LeftColumns");
		String rightColumnList = _Session.getAttribute(operation, "RightColumns");
		
		if (StringUtilities.isNullOrEmpty(leftColumnList)) {
			throw new RuntimeException("Join requires at least one column name in LeftColumns.");
		} else if (StringUtilities.isNullOrEmpty(rightColumnList)) {
			throw new RuntimeException("Join requires at least one column name in RightColumns.");
		}

		_leftColumnNames = StringUtilities.split(leftColumnList);
		_rightColumnNames = StringUtilities.split(rightColumnList);
		
		if (_leftColumnNames.length != _rightColumnNames.length){
			throw new RuntimeException(String.format("The number of columns on the left and right must be match (%d left columns != %d right columns)", _leftColumnNames.length, _rightColumnNames.length));
		}

//		_numberOfKeys = columnNames.length;
//		_inputColumnIndexes = new int[_numberOfKeys];
//		_isAscending = new boolean[_numberOfKeys];
//
//		// Using the file offset to ensure that the sort is "stable".
//		// Offset column name is ixStreamOffset
//		_columnNames = new String[_numberOfKeys + 1];
//		_columnNames[_numberOfKeys] = "ixStreamOffset";
//		_dataTypes = new DataType[_numberOfKeys + 1];
//		_dataTypes[_numberOfKeys] = DataType.LongData;
//
//		if (sortDirections == null) {
//			for (int i = 0; i < _numberOfKeys; i++) {
//				_isAscending[i] = true;
//				_columnNames[i] = columnNames[i];
//			}
//		} else {
//			int length = Math.min(sortDirections.length, _numberOfKeys);
//			for (int i = 0; i < length; i++) {
//				_columnNames[i] = columnNames[i].trim();
//				String direction = sortDirections[i];
//				if (StringUtilities.isNullOrEmpty(direction) || direction.toLowerCase().trim().startsWith("asc")) {
//					_isAscending[i] = true;
//				} else {
//					_isAscending[i] = false;
//				}
//			}
//		}
//
//		// Added to log the instructions defined:
//		StringBuilder sortRequest = new StringBuilder();
//		for (int i = 0; i < _numberOfKeys; i++) {
//			if (i > 0)
//				sortRequest.append(", ");
//			sortRequest.append(_columnNames[i]);
//			sortRequest.append(_isAscending[i] ? " ASC" : " DESC");
//		}
//		_TransformInfo.appendFormat("Order by: %s", sortRequest.toString());
		
	}

	@Override
	public boolean isTableLevel() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public Object[] processDataRow(Object[] dataRow) {
		throw new RuntimeException("Join requires access to the entire data set.  It cannot be combined with other data transformations.");
	}

	@Override
	public DataStream processDataStream(DataStream inputStream, int memoryLimit) {
		DataStream outputStream = null;
		
		// Get the right side data (create new instance of data engine)
		
		// Sort the right and left dataStreams on the join columns
		
		// Join the data and write the final file.
		
		return outputStream;
	}
}
