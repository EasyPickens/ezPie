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

package com.fanniemae.ezpie.datafiles;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.common.CryptoUtilities;
import com.fanniemae.ezpie.common.DataStream;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.common.XmlUtilities;
import com.fanniemae.ezpie.datafiles.lowlevel.BinaryInputStream;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFormat;
import com.fanniemae.ezpie.datafiles.lowlevel.DataRow;
import com.fanniemae.ezpie.datafiles.lowlevel.FieldBoolean;
import com.fanniemae.ezpie.datafiles.lowlevel.FieldByte;
import com.fanniemae.ezpie.datafiles.lowlevel.FieldChar;
import com.fanniemae.ezpie.datafiles.lowlevel.FieldDate;
import com.fanniemae.ezpie.datafiles.lowlevel.FieldDouble;
import com.fanniemae.ezpie.datafiles.lowlevel.FieldInteger;
import com.fanniemae.ezpie.datafiles.lowlevel.FieldLong;
import com.fanniemae.ezpie.datafiles.lowlevel.FieldObject;
import com.fanniemae.ezpie.datafiles.lowlevel.FieldReadWrite;
import com.fanniemae.ezpie.datafiles.lowlevel.FieldString;
import com.fanniemae.ezpie.datafiles.lowlevel.FieldStringEncrypted;
import com.fanniemae.ezpie.datafiles.lowlevel.FieldUUID;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.ColumnTypes;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2015-12-28
 * 
 */

public class DataReader extends DataFormat {
	private final BinaryInputStream _bis;
	private FieldReadWrite[] _readMethods;

	protected DataReader _drSourceData = null;
	protected long _endOfDataBlock;
	protected String[][] _dataSchema = new String[][] {};

	public DataReader(DataStream ds) throws IOException {
		if (ds.IsMemory()) {
			_bis = new BinaryInputStream(ds.getMemorystream());
		} else {
			_filename = ds.getFilename();
			_bis = new BinaryInputStream(_filename);
		}
		initialize();
	}

	public DataReader(String filename) throws IOException {
		_filename = filename;
		_bis = new BinaryInputStream(filename);
		initialize();
	}

	public DataReader(byte[] byteArrayMemoryStream) throws IOException {
		_bis = new BinaryInputStream(byteArrayMemoryStream);
		initialize();
	}

	@Override
	public void close() throws Exception {
		if ((_bis != null) && (!_disposed)) {
			_bis.close();
			_disposed = true;
		}
		if (_drSourceData != null) {
			_drSourceData.clone();
		}
	}

	public boolean eof() throws IOException {
		return _bis.getPosition() >= _endOfDataBlock;
	}

	public String getFilename() {
		return _filename;
	}

	public DataRow getDataRowAndSchemaAt(long offSet) throws IOException {
		_bis.seek(offSet);
		return getDataRowAndSchema();
	}

	public DataRow getDataRowAndSchema() throws IOException {
		_dataRow.setValues(getDataRow());
		if (_byteFileType == 0) {
			return _dataRow;
		}
		return _drSourceData.getDataRowAndSchemaAt((long) _dataRow.getValues()[0]);
	}

	public void moveToRow(long rowNumber) throws IOException {
		rowNumber = (rowNumber <= 0L) ? 1L : rowNumber;

		_currentRowNumber = _firstRow - 1L;
		if ((rowNumber > _fullRowCount) || (rowNumber > _lastRow)) {
			_bis.seek(_endOfDataBlock);
			_currentRowNumber = _lastRow;
		} else if (rowNumber == 1L) {
			_bis.seek(_startOfDataBlock);
		} else if (_indexBlock.size() > 0) {
			long lRowNum = _firstRow - 1L;
			long lPosition = _startOfDataBlock;
			int iLength = _indexBlock.size();
			for (int i = 0; i < iLength; i++) {
				if (_indexBlock.get(i).getRowNumber() + _firstRow - 1L < rowNumber) {
					lRowNum = _indexBlock.get(i).getRowNumber() + _firstRow - 1L;
					lPosition = _indexBlock.get(i).getOffSet();
				} else {
					break;
				}
			}
			_currentRowNumber = lRowNum;
			_bis.seek(lPosition);
			while (!eof() && (_currentRowNumber < rowNumber)) {
				skipDataRow();
			}
		} else {
			while (!eof() && (_currentRowNumber < rowNumber)) {
				skipDataRow();
				// getRowValues();
			}
		}
	}

	public void skipDataRow(long numberOfRows) throws IOException {
		for (int i = 0; i < numberOfRows; i++) {
			skipDataRow();
		}
	}

	public void skipDataRow() throws IOException {
		// Just advancing the file pointer, no need to load the values.
		for (int i = 0; i < _dataRow.getColumnCount(); i++) {
			if (_dataRow.getColumnType(i) == ColumnTypes.DataValue) {
				_readMethods[i].Read();
			}
		}
		_currentRowNumber++;
	}

	public Object[] getDataRowAt(long position) throws IOException {
		_bis.seek(position);
		return getDataRow();
	}

	public Object[] getDataRow() throws IOException {
		int length = _dataRow.getColumnCount();
		Object[] aData = new Object[length];
		for (int i = 0; i < length; i++) {
			if (_dataRow.getColumnType(i) == ColumnTypes.GlobalValue) {
				aData[i] = _dataRow.getValue(i);
			} else {
				aData[i] = _readMethods[i].Read();
			}
		}
		_currentRowNumber++;
		return aData;
	}

	public Object getHeaderInformation(DataFileEnums.BinaryFileInfo key) {
		return _HeaderInformation.get(key);
	}

	public Map<DataFileEnums.BinaryFileInfo, Object> getHeader() {
		return _HeaderInformation;
	}

	public String[] getColumnNames() {
		return _dataRow.getColumnNames();
	}

	public DataType[] getDataTypes() {
		return _dataRow.getDataTypes();
	}

	public long getPosition() throws IOException {
		return _bis.getPosition();
	}

	public String[][] getSchema() {
		return _dataSchema;
	}

	protected void initialize() throws IOException {
		readHeader();
		int length = _dataRow.getColumnCount();
		_readMethods = new FieldReadWrite[length];
		for (int i = 0; i < _readMethods.length; i++) {
			_readMethods[i] = getReadMethod(_dataRow.getDataType(i));
		}
	}

	protected void readHeader() throws IOException {
		_byteFileType = _bis.readByte();
		_isEncrypted = _bis.readBoolean();
		_fingerPrint = _bis.readUTF();
		_sourceDataFilename = _bis.readUTF();
		_fullRowCountKnown = _bis.readBoolean();
		_fullRowCount = _bis.readLong();
		_firstRow = _bis.readLong();
		_lastRow = _bis.readLong();
		_indexStart = _bis.readLong();
		_schemaStart = _bis.readLong();
		_dateCreated = new Date(_bis.readLong());
		_dateExpires = new Date(_bis.readLong());
		_endOfDataBlock = _indexStart;
		_startOfDataBlock = _bis.getPosition();

		// Jump to the Index
		_bis.seek(_indexStart);
		_indexBlock = new ArrayList<>();
		while (_bis.getPosition() < _schemaStart) {
			IndexEntry ie = new IndexEntry();
			ie.setRowNumber(_bis.readLong());
			ie.setOffSet(_bis.readLong());
			_indexBlock.add(ie);
		}

		// Jump to Schema and load it.
		_bis.seek(_schemaStart);
		_schemaXML = _bis.readUTF();
		if (_isEncrypted) {
			_schemaXML = CryptoUtilities.EncryptDecrypt(_schemaXML);
		}

		populateHeaderInformation();

		try {
			Document doc = XmlUtilities.createXMLDocument(_schemaXML);
			XPathFactory xFactory = XPathFactory.newInstance();
			XPath xp = xFactory.newXPath();
			XPathExpression expr = xp.compile("FileInfo/DataInfo/DataColumn");

			NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
			if (nodes.getLength() == 0) {
				throw new IOException("Data file does not contain any column information.");
			}

			_dataRow = new DataRow(nodes.getLength());
			_dataSchema = new String[nodes.getLength()][2];
			for (int i = 0; i < nodes.getLength(); i++) {
				Element elementColumn = (Element) nodes.item(i);

				DataFileEnums.ColumnTypes eColType = DataFileEnums.ColumnTypes.DataValue;

				String name = elementColumn.getAttribute("Name");
				String dataType = elementColumn.getAttribute("DataType");
				String sGlobalValue = elementColumn.getAttribute("GlobalValue");
				Object globalValue = null;
				if ("GlobalValue".equalsIgnoreCase(elementColumn.getAttribute("ColumnType"))) {
					eColType = DataFileEnums.ColumnTypes.GlobalValue;
					globalValue = StringUtilities.toObject(dataType, sGlobalValue);
				}
				_dataRow.DefineColumn(i, name, eColType, dataType, globalValue);
				_dataSchema[i][0] = name;
				_dataSchema[i][1] = dataType;
			}
		} catch (XPathExpressionException | IOException ex) {
			throw new IOException("Error reading data file header. ", ex);
		}

		// If this is a view file, check for source data.
		if (_byteFileType == 1) {
			File fd = new File(_filename);
			File dir = fd.getParentFile();
			String dataFilename = dir.getParent();
			if (!dataFilename.endsWith(java.io.File.separator)) {
				dataFilename += java.io.File.separator;
			}
			dataFilename += _sourceDataFilename;
			if (FileUtilities.isInvalidFile(dataFilename)) {
				throw new IOException(String.format("Could not find the %s data file.", dataFilename));
			}

			_drSourceData = new DataReader(dataFilename);
			// Check to be sure the finger print still matches.
			if (!_fingerPrint.equals(_drSourceData.getFingerPrint())) {
				// This file must be recreated based on the new data.
			}
		}
		_bis.seek(_startOfDataBlock);
	}

	private FieldReadWrite getReadMethod(DataType columnDataType) throws IOException {
		// Simplified code to convert some types into others. E.g. Byte, Int16, SByte ==> Int32
		switch (columnDataType) {
		case BooleanData:
			return new FieldBoolean(_bis);
		case ByteData:
			return new FieldByte(_bis);
		case CharData:
			return new FieldChar(_bis);
		case DateData:
			return new FieldDate(_bis);
		case DoubleData:
			return new FieldDouble(_bis);
		case UUIDData:
			return new FieldUUID(_bis);
		case IntegerData:
			return new FieldInteger(_bis);
		case LongData:
			return new FieldLong(_bis);
		case StringData:
			if (_isEncrypted) {
				return new FieldStringEncrypted(_bis);
			} else {
				return new FieldString(_bis);
			}
		case ObjectData:
			return new FieldObject(_bis);
		default:
			throw new IOException("Data type " + columnDataType.toString() + " is not currently supported by the data engine.");
		}
	}
}
