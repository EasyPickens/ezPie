/**
 * 
 * Copyright (c) 2017 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 * 
**/

package com.fanniemae.ezpie.data.utilities;

import org.apache.poi.ss.util.CellReference;

import com.fanniemae.ezpie.common.StringUtilities;

/**
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2017-05-22
 * 
 */

public class ExcelRange {
	protected final String NUMBER_CHECK = ".*[0-9].*";
	protected final String LETTER_CHECK = ".*[A-Z].*";
	
	protected CellReference _topLeftCell = null;
	protected CellReference _bottomRightCell = null;
	
	protected String _leftColumn = null;
	protected String _rightColumn = null;
	
	protected int _leftRow = -1;
	protected int _rightRow = -1;

	public ExcelRange(String cellRange) {
		if (StringUtilities.isNullOrEmpty(cellRange)) {
			return;
		}

		int pos = cellRange.indexOf(':');
		if (pos > 0) {
			String topLeftCell = cellRange.substring(0, pos);
			if (!topLeftCell.matches(LETTER_CHECK) && topLeftCell.matches(NUMBER_CHECK)) {
				topLeftCell = "A" + topLeftCell;
			} else if (topLeftCell.matches(LETTER_CHECK) && !topLeftCell.matches(NUMBER_CHECK)) {
				topLeftCell += "1";
			}
			_topLeftCell = new CellReference(topLeftCell);

			String bottomRightCell = cellRange.substring(pos + 1);
			if (bottomRightCell.matches(LETTER_CHECK) && bottomRightCell.matches(NUMBER_CHECK)) {
				_bottomRightCell = new CellReference(bottomRightCell);
			}

			return;
		}
		
		String topLeftCell = cellRange;
		if (!topLeftCell.matches(LETTER_CHECK) && topLeftCell.matches(NUMBER_CHECK)) {
			topLeftCell = "A" + topLeftCell;
		} else if (topLeftCell.matches(LETTER_CHECK) && !topLeftCell.matches(NUMBER_CHECK)) {
			topLeftCell += "1";
		}
		_topLeftCell = new CellReference(topLeftCell);
		return;
	}

	public CellReference getStartCell() {
		return _topLeftCell;
	}

	public CellReference getEndCell() {
		return _bottomRightCell;
	}

	public String getStartColumn() {
		if (_topLeftCell == null)
			return "A";
		return CellReference.convertNumToColString(_topLeftCell.getCol());
	}

	public int getEndColumn() {
		if (_bottomRightCell == null)
			return -1;
		return _bottomRightCell.getCol();
	}

	public int getStartRow() {
		if (_topLeftCell == null)
			return 1;
		return _topLeftCell.getRow();
	}

	public int getEndRow() {
		if (_bottomRightCell == null)
			return -1;
		return _bottomRightCell.getRow();
	}
}
