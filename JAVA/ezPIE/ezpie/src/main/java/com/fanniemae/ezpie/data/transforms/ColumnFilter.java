/**
 *  
 * Copyright (c) 2017 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.data.transforms;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.ArrayUtilities;
import com.fanniemae.ezpie.common.StringUtilities;

/**
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2017-08-18
 * 
 */

public class ColumnFilter extends DataTransform {

	protected int _columnCount = 0;

	protected String[] _removeColumnNames = null;
	protected String[] _keepColumnNames = null;

	protected List<Integer> _skipIndexes = null;
	protected List<Integer> _keepIndexes = null;

	public ColumnFilter(SessionManager session, Element transform) {
		super(session, transform, false);

		String removeColumns = this.getOptionalAttribute("RemoveColumns");
		String keepColumns = this.getOptionalAttribute("KeepColumns");
		if (StringUtilities.isNullOrEmpty(removeColumns) && StringUtilities.isNullOrEmpty(keepColumns)) {
			throw new RuntimeException("ColumnFilter requires a value for either RemoveColumns or KeepColumns.");
		} else if (StringUtilities.isNotNullOrEmpty(removeColumns) && StringUtilities.isNotNullOrEmpty(keepColumns)) {
			throw new RuntimeException("ColumnFilter requires a value for either RemoveColumns or KeepColumns. Both attributes cannot be used in the same action element.");
		}

		if (StringUtilities.isNotNullOrEmpty(removeColumns)) {
			_removeColumnNames = StringUtilities.split(removeColumns);
		}

		if (StringUtilities.isNotNullOrEmpty(keepColumns)) {
			_keepColumnNames = StringUtilities.split(keepColumns);
		}
	}

	@Override
	public Object[] processDataRow(Object[] dataRow) {
		int pos = 0;
		Object[] newDataRow = new Object[_columnCount];
		if ((_removeColumnNames != null) && (_removeColumnNames.length > 0)) {
			for (int i = 0; i < dataRow.length; i++) {
				if (!_skipIndexes.contains(i)) {
					newDataRow[pos] = dataRow[i];
					pos++;
				}
			}
		} else if ((_keepColumnNames != null) && (_keepColumnNames.length > 0)) {
			for (int i = 0; i < dataRow.length; i++) {
				if (_keepIndexes.contains(i)) {
					newDataRow[pos] = dataRow[i];
					pos++;
				}
			}
		}
		return newDataRow;
	}

	@Override
	public String[][] UpdateSchema(String[][] inputSchema) {
		_inputSchema = inputSchema;
		int length = 0;
		int pos = 0;

		if ((_removeColumnNames != null) && (_removeColumnNames.length > 0)) {
			length = _removeColumnNames.length;
			_skipIndexes = new ArrayList<Integer>();
			for (int i = 0; i < length; i++) {
				int colIndex = ArrayUtilities.indexOf(inputSchema, _removeColumnNames[i], true);
				if (colIndex > -1) {
					_skipIndexes.add(colIndex);
				}
			}

			length = _skipIndexes.size();
			if (length == 0) {
				return inputSchema;
			}

			String[][] newSchema = new String[inputSchema.length - length][2];
			for (int i = 0; i < inputSchema.length; i++) {
				if (!_skipIndexes.contains(i)) {
					newSchema[pos][0] = inputSchema[i][0];
					newSchema[pos][1] = inputSchema[i][1];
					pos++;
				}
			}
			_columnCount = newSchema.length;
			return newSchema;

		} else if ((_keepColumnNames != null) && (_keepColumnNames.length > 0)) {
			length = _keepColumnNames.length;
			_keepIndexes = new ArrayList<Integer>();
			for (int i = 0; i < length; i++) {
				int colIndex = ArrayUtilities.indexOf(inputSchema, _keepColumnNames[i], true);
				if (colIndex > -1) {
					_keepIndexes.add(colIndex);
				}
			}

			length = _keepIndexes.size();
			if (length == 0) {
				return inputSchema;
			}

			String[][] newSchema = new String[length][2];
			for (int i = 0; i < inputSchema.length; i++) {
				if (_keepIndexes.contains(i)) {
					newSchema[pos][0] = inputSchema[i][0];
					newSchema[pos][1] = inputSchema[i][1];
					pos++;
				}
			}
			_columnCount = newSchema.length;
			return newSchema;
		}

		return inputSchema;
	}

}
