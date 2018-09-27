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

package com.fanniemae.ezpie.data.connectors;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.ArrayUtilities;
import com.fanniemae.ezpie.common.Constants;
import com.fanniemae.ezpie.common.DataUtilities;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.common.XmlUtilities;
import com.fanniemae.ezpie.data.utilities.ExcelRange;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2017-05-22
 * 
 */

public class ExcelConnector extends DataConnector {

	protected final String SOURCE_FILENAME_COLUMN = "Source_Filename";

	protected String _filename;
	protected String _filenameOnly;
	protected String _sheetName;
	protected String _columnNameAddress;
	protected String _dataAddress;

	protected boolean _addFilename = false;
	protected boolean _allTypesStrings = true;

	protected int _columnCount;
	protected int[] _sourceIndex;

	protected Object[] _dataRow;
	protected DataType[] _dataTypes;

	protected FileInputStream _fis;
	protected Workbook _workbook;
	protected Sheet _sheet;
	protected ExcelRange _headerRange;
	protected ExcelRange _dataCellRange;

	protected List<String> _columnLabels;
	protected List<String> _columnAddress;

	// support for data extraction
	protected Iterator<Row> _iterator = null;
	protected int _startRow = 1;
	protected int _startColumnIndex = 1;
	protected int _endRow = -1;
	protected int _endColumnIndex = -1;
	protected int _currentExcelRowIndex = -1;

	public ExcelConnector(SessionManager session, Element dataSource, Boolean isSchemaOnly) {
		super(session, dataSource, isSchemaOnly);
	}

	@Override
	public Boolean open() {
		_filename = _session.requiredAttribute(_dataSource, "Filename");
		_sheetName = _session.optionalAttribute(_dataSource, "SheetName");
		_columnNameAddress = _session.optionalAttribute(_dataSource, "ColumnNameLocation");
		_dataAddress = _session.optionalAttribute(_dataSource, "DataLocation");
		_addFilename = StringUtilities.toBoolean(_session.optionalAttribute(_dataSource, "AddFilenameColumn"), false);
		_allTypesStrings = StringUtilities.toBoolean(_session.optionalAttribute(_dataSource, "AllStrings"), true);

		try {
			File file = new File(_filename);
			if (file.exists() && file.isFile()) {
				_session.addLogMessage("", "Excel File Size", String.format("%,d bytes", file.length()));
			}
			_filenameOnly = file.getName();
			_fis = new FileInputStream(file);
			_workbook = new XSSFWorkbook(_fis);

			StringBuilder sb = new StringBuilder();
			String newLine = System.getProperty("line.separator");
			int numberSheets = _workbook.getNumberOfSheets();
			for (int i = 0; i < numberSheets; i++) {
				if (i > 0)
					sb.append(newLine);
				sb.append(String.format("%s", _workbook.getSheetName(i)));
			}
			_session.addLogMessage("", "Worksheets Found", sb.toString());
			_sheet = StringUtilities.isNotNullOrEmpty(_sheetName) ? _workbook.getSheet(_sheetName) : _workbook.getSheetAt(0);
			if (_sheet == null) {
				_session.addLogMessage(Constants.LOG_WARNING_MESSAGE, "Worksheet", String.format("%s worksheet not found, Skipping this file.", _sheetName));
				return false;
			}
			readSchema();

			if (_addFilename) {
				_dataSchema = (String[][]) ArrayUtilities.resizeArray(_dataSchema, _dataSchema.length + 1);
				_dataSchema[_dataSchema.length - 1] = new String[] { SOURCE_FILENAME_COLUMN, "String" };
				_dataTypes = (DataType[]) ArrayUtilities.resizeArray(_dataTypes, _dataTypes.length + 1);
				_dataTypes[_dataTypes.length - 1] = DataType.StringData;
				_columnCount = _dataSchema.length;
			}

			StringBuilder schemaReport = new StringBuilder();
			for (int i = 0; i < _dataSchema.length; i++) {
				if (i > 0)
					schemaReport.append(newLine);
				schemaReport.append(String.format("%s (%s)", _dataSchema[i][0], _dataSchema[i][1]));
			}
			_session.addLogMessage("", "Data Schema", schemaReport.toString());
		} catch (FileNotFoundException e) {
			throw new PieException(String.format("%s file not found.", _filename), e);
		} catch (Exception e) {
			throw new PieException(String.format("Problem reading the workbook or sheet. %s", e.getMessage()), e);
		}
		return null;
	}

	@Override
	public Boolean eof() {
		if (_sheet == null) {
			return true;
		} else if (_iterator != null) {
			return !_iterator.hasNext();
		}

		return !(_currentExcelRowIndex < _endRow);
	}

	@Override
	public Object[] getDataRow() {
		Row excelDataRow = null;
		if (_sheet == null) {
			return null;
		} else if (_iterator != null) {
			excelDataRow = _iterator.next();
		} else {
			excelDataRow = _sheet.getRow(_currentExcelRowIndex);
		}
		_currentExcelRowIndex++;
		if (excelDataRow == null) {
			_currentExcelRowIndex = Integer.MAX_VALUE;
			return null;
		} else {
			_dataRow = readExcelData(excelDataRow);
			return _dataRow;
		}
	}

	@Override
	public void close() {
		if (_workbook != null) {
			try {
				_workbook.close();
			} catch (IOException ex) {
				throw new RuntimeException("Could not close the workbook object.", ex);
			}
		}

		if (_fis != null) {
			try {
				_fis.close();
			} catch (IOException ex) {
				throw new RuntimeException("Could not close file input stream.", ex);
			}
		}

	}

	protected void readSchema() {
		if (userDefinedColumns())
			return;
		readColumnNames();
		if (_allTypesStrings) {
			setSchemaToStrings();
		} else {
			readColumnTypes();
		}
		return;
	}

	protected boolean userDefinedColumns() {
		NodeList nl = XmlUtilities.selectNodes(_dataSource, "Column");
		_columnCount = nl.getLength();
		if (nl.getLength() > 0) {
			_dataSchema = new String[_columnCount][2];
			_dataTypes = new DataType[_columnCount];
			_columnAddress = new ArrayList<String>();
			for (int i = 0; i < _columnCount; i++) {
				Element columnElement = (Element) nl.item(i);

				_dataSchema[i][0] = _session.requiredAttribute(columnElement, "Name");
				_dataSchema[i][1] = (_allTypesStrings) ? "String" : _session.requiredAttribute(columnElement, "DataType");
				_columnAddress.add(_session.requiredAttribute(columnElement, "ColumnLetter"));
				_dataTypes[i] = DataUtilities.dataTypeToEnum(_dataSchema[i][1]);
			}
			return true;
		}
		return false;
	}

	protected void readColumnNames() {
		// read the schema from the first row or the named row.
		_headerRange = null;
		Row headerRow = null;
		if (StringUtilities.isNotNullOrEmpty(_columnNameAddress)) {
			_headerRange = new ExcelRange(_columnNameAddress);
			CellReference cr = _headerRange.getStartCell();
			headerRow = _sheet.getRow(cr.getRow());
		} else {
			Iterator<Row> iterator = _sheet.iterator();
			headerRow = iterator.next();
		}

		_columnCount = 0;
		int endColumnIndex = (_headerRange == null) ? -1 : _headerRange.getEndColumn();
		String value = "";
		_columnLabels = new ArrayList<String>();
		_columnAddress = new ArrayList<String>();
		List<String> usedLabels = new ArrayList<String>();
		for (Cell cell : headerRow) {
			String columnLetter = CellReference.convertNumToColString(cell.getColumnIndex());
			if ((endColumnIndex != -1) && (cell.getColumnIndex() > endColumnIndex)) {
				break;
			}

			CellType ct = cell.getCellTypeEnum();
			if (ct == CellType.FORMULA)
				ct = cell.getCachedFormulaResultTypeEnum();
			switch (ct) {
			case STRING:
				value = cell.getStringCellValue();
				if (usedLabels.contains(value.toLowerCase()))
					value = String.format("%s_%s%d", value, columnLetter, cell.getRowIndex() + 1);
				else
					usedLabels.add(value.toLowerCase());
				_columnLabels.add(value);
				_columnAddress.add(columnLetter);
				break;
			case BOOLEAN:
			case NUMERIC:
			case FORMULA:
			case BLANK:
				_columnLabels.add(String.format("Column_%s", columnLetter));
				_columnAddress.add(columnLetter);
				break;
			default:
				break;
			}
		}

		_columnCount = _columnLabels.size();
		_dataSchema = new String[_columnCount][2];
		_dataTypes = new DataType[_columnCount];
		for (int i = 0; i < _columnCount; i++) {
			_dataSchema[i][0] = _columnLabels.get(i);
		}
	}

	protected void readColumnTypes() {
		// read the schema from the first row or the named row.
		_dataCellRange = null;
		Row dataRow = null;
		if (StringUtilities.isNotNullOrEmpty(_dataAddress)) {
			setupDataRange();
			for (int currentRowNumber = _startRow; currentRowNumber < _endRow; currentRowNumber++) {
				dataRow = _sheet.getRow(currentRowNumber);
				if (dataRow == null)
					break;
				readRowSchema(dataRow, _endColumnIndex);
			}
			// reset to the first row to pull data
			_currentExcelRowIndex = _startRow;
		} else {
			_iterator = _sheet.iterator();
			int rowCount = 0;
			while (_iterator.hasNext()) {
				dataRow = _iterator.next();
				if (dataRow == null)
					break;
				if ((rowCount == 0) && (_headerRange == null)) {
					continue;
				}
				readRowSchema(dataRow, -1);
			}
			// start at the first row again
			_iterator = _sheet.iterator();
		}
	}

	protected void readRowSchema(Row dataRow, int endColumnIndex) {
		String cellAddress = "";
		String schemaColumnType = null;
		String currentCellDataType = null;
		try {
			for (Cell cell : dataRow) {
				cellAddress = cell.getAddress().toString();
				String columnLetter = CellReference.convertNumToColString(cell.getColumnIndex());
				int columnIndex = _columnAddress.indexOf(columnLetter);
				if (columnIndex == -1) {
					continue;
				}

				currentCellDataType = "String";
				CellType ct = cell.getCellTypeEnum();
				if (ct == CellType.FORMULA)
					ct = cell.getCachedFormulaResultTypeEnum();
				switch (ct) {
				case STRING:
					currentCellDataType = "String";
					break;
				case BOOLEAN:
					currentCellDataType = "Boolean";
					break;
				case NUMERIC:
					currentCellDataType = "Double";
					break;
				case FORMULA:
					currentCellDataType = "Object";
					break;
				case BLANK:
					currentCellDataType = "String";
					break;
				default:
					break;
				}

				// Object, String, Numeric, Boolean
				schemaColumnType = _dataSchema[columnIndex][1];
				if (schemaColumnType == null) {
					_dataSchema[columnIndex][1] = currentCellDataType;
				} else if ("Object".equals(schemaColumnType)) {
					// no change
				} else if ("String".equals(schemaColumnType) && "Object".equals(currentCellDataType)) {
					_dataSchema[columnIndex][1] = currentCellDataType;
				} else if ("Double".equals(schemaColumnType) && "Object|String".contains(currentCellDataType)) {
					_dataSchema[columnIndex][1] = currentCellDataType;
				} else if ("Boolean".equals(schemaColumnType) && "Object|String|Double".contains(currentCellDataType)) {
					_dataSchema[columnIndex][1] = currentCellDataType;
				}
				_dataTypes[columnIndex] = DataUtilities.dataTypeToEnum(_dataSchema[columnIndex][1]);
				columnIndex++;
			}
		} catch (Exception ex) {
			throw new PieException(String.format("Error while reading Excel cell %s for its data type (%s). %s", cellAddress, currentCellDataType, ex.getMessage()), ex);
		}
	}

	protected Object[] readExcelData(Row excelDataRow) {
		Object[] data = new Object[_columnCount];
		String cellAddress = "";
		int dataIndex = 0;
		try {
			for (Cell cell : excelDataRow) {
				cellAddress = cell.getAddress().toString();
				String columnLetter = CellReference.convertNumToColString(cell.getColumnIndex());
				int columnIndex = _columnAddress.indexOf(columnLetter);
				if (columnIndex == -1) {
					continue;
				}

				CellType ct = cell.getCellTypeEnum();
				if (ct == CellType.FORMULA)
					ct = cell.getCachedFormulaResultTypeEnum();
				switch (ct) {
				case STRING:
					data[dataIndex] = cell.getStringCellValue();
					break;
				case BOOLEAN:
					data[dataIndex] = _allTypesStrings ? Boolean.toString(cell.getBooleanCellValue()) : cell.getBooleanCellValue();
					break;
				case NUMERIC:
					data[dataIndex] = _allTypesStrings ? Double.toString(cell.getNumericCellValue()) : cell.getNumericCellValue();
					break;
				default:
					data[dataIndex] = _allTypesStrings ? "" : null;
					break;
				}
				dataIndex++;
			}
			if (_addFilename)
				data[data.length - 1] = _filenameOnly;
		} catch (Exception ex) {
			throw new PieException(String.format("Error while reading Excel data from cell %s. %s", cellAddress, ex.getMessage()), ex);
		}

		return data;
	}

	protected void setSchemaToStrings() {
		for (int i = 0; i < _dataSchema.length; i++) {
			_dataSchema[i][1] = "String";
		}
		setupDataRange();
	}

	protected void setupDataRange() {
		_dataCellRange = null;
		if (StringUtilities.isNotNullOrEmpty(_dataAddress)) {
			_dataCellRange = new ExcelRange(_dataAddress);
			CellReference cr = _dataCellRange.getStartCell();
			_startColumnIndex = cr.getCol();
			_startRow = cr.getRow();
			_endRow = (_dataCellRange.getEndRow() == -1) ? _sheet.getLastRowNum() : _dataCellRange.getEndRow();
			_endColumnIndex = _dataCellRange.getEndColumn();
			_currentExcelRowIndex = _startRow;
		} else {
			_iterator = _sheet.iterator();
		}
	}
}
