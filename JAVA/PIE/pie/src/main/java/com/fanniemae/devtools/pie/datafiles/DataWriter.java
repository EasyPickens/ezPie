/**
 *  
 * Copyright (c) 2015 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPIE/wiki/Fannie-Mae-Open-Source-Licensing-Project
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.devtools.pie.datafiles;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.common.CryptoUtilities;
import com.fanniemae.devtools.pie.common.DataStream;
import com.fanniemae.devtools.pie.common.StringUtilities;
import com.fanniemae.devtools.pie.common.XmlUtilities;
import com.fanniemae.devtools.pie.datafiles.lowlevel.BinaryOutputStream;
import com.fanniemae.devtools.pie.datafiles.lowlevel.DataFileEnums;
import com.fanniemae.devtools.pie.datafiles.lowlevel.DataFileEnums.DataType;
import com.fanniemae.devtools.pie.datafiles.lowlevel.DataFormat;
import com.fanniemae.devtools.pie.datafiles.lowlevel.DataRow;
import com.fanniemae.devtools.pie.datafiles.lowlevel.FieldBigDecimal;
import com.fanniemae.devtools.pie.datafiles.lowlevel.FieldBoolean;
import com.fanniemae.devtools.pie.datafiles.lowlevel.FieldByte;
import com.fanniemae.devtools.pie.datafiles.lowlevel.FieldChar;
import com.fanniemae.devtools.pie.datafiles.lowlevel.FieldDate;
import com.fanniemae.devtools.pie.datafiles.lowlevel.FieldDouble;
import com.fanniemae.devtools.pie.datafiles.lowlevel.FieldFloat;
import com.fanniemae.devtools.pie.datafiles.lowlevel.FieldInteger;
import com.fanniemae.devtools.pie.datafiles.lowlevel.FieldLong;
import com.fanniemae.devtools.pie.datafiles.lowlevel.FieldReadWrite;
import com.fanniemae.devtools.pie.datafiles.lowlevel.FieldShort;
import com.fanniemae.devtools.pie.datafiles.lowlevel.FieldSqlTimestamp;
import com.fanniemae.devtools.pie.datafiles.lowlevel.FieldString;
import com.fanniemae.devtools.pie.datafiles.lowlevel.FieldStringEncrypted;
import com.fanniemae.devtools.pie.datafiles.lowlevel.FieldUUID;
import com.fanniemae.devtools.pie.datafiles.lowlevel.FieldObject;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2015-12-28
 * 
 */

public class DataWriter extends DataFormat {
	private final BinaryOutputStream _bos;
	protected FieldReadWrite[] _writeMethods = null;

	protected int _columnCount = 0;
	protected Map<String, String[]> _globalValues = new HashMap<>();
	protected Map<String, List<String[]>> _columProfiles = new HashMap<>();

	public DataWriter(String filename) throws IOException {
		this(filename, 20, "", null, false);
	}

	public DataWriter(String filename, boolean isDynamicSqlBuffer) throws IOException {
		this(filename, 0, "", null, isDynamicSqlBuffer);
	}

	public DataWriter(String filename, int memoryLimitInMegabytes) throws IOException {
		this(filename, memoryLimitInMegabytes, "", null, false);
	}

	public DataWriter(String filename, int memoryLimitInMegabytes, boolean isDynamicSqlBuffer) throws IOException {
		this(filename, memoryLimitInMegabytes, "", null, isDynamicSqlBuffer);
	}

	public DataWriter(String filename, int memoryLimitInMegabytes, String sourceDataFilename, UUID fingerPrint, boolean isDynamicSqlBuffer) throws IOException {
		_filename = filename;
		_sourceDataFilename = sourceDataFilename;
		_byteFileType = 1;

		if (isDynamicSqlBuffer) {
			_indexInterval = 500L; // Used by index to determine how often to add
								// entry.
			_nextBreak = 500L; // Next row count to add an index entry.
		}

		if (fingerPrint == null) {
			_byteFileType = 0;
			_fingerPrint = UUID.randomUUID().toString();
		} else {
			_fingerPrint = fingerPrint.toString();
		}

		// If this is a View file, then get the data filename only.
		if (StringUtilities.isNotNullOrEmpty(_sourceDataFilename) && _sourceDataFilename.contains(File.separator)) {
			File oFile = new File(_sourceDataFilename);
			_sourceDataFilename = oFile.getName();
		}

		_bos = new BinaryOutputStream(_filename, memoryLimitInMegabytes, _fingerPrint);
		writeInitialHeader(); // Place holder for final information.
	}

	@Override
	public void close() throws IOException {
		if ((_bos != null) && (!_disposed)) {
			try {
				writeFooter();
				writeFinalHeader();
			} finally {
				_bos.close();
				_disposed = true;
			}
		}
	}

	public void setDataColumns(String[] columnNames, DataType[] dataTypes) throws IOException {
		_dataRow = new DataRow(columnNames.length);
		for (int i = 0; i < columnNames.length; i++) {
			defineDataColumn(i, columnNames[i], dataTypes[i]);
		}
		setupColumnWriters();
	}

	public void setDataColumns(String[][] columnNamesAndTypes) throws IOException {
		int columnCount = columnNamesAndTypes.length;
		_dataRow = new DataRow(columnCount);

		for (int i = 0; i < columnCount; i++) {
			defineDataColumn(i, columnNamesAndTypes[i][0], columnNamesAndTypes[i][1]);
		}
		setupColumnWriters();
	}

	public void setGlobalValue(String columnName, String dataType, String value) {
		_globalValues.put(columnName, new String[] { dataType, value });
	}

	public void setColumnProfile(String columnName, List<String[]> profile) {
		_columProfiles.put(columnName, profile);
	}

	public void writeDataRow(Object[] data) throws IOException {
		if (_currentRowNumber == _nextBreak) {
			IndexEntry ie = new IndexEntry();
			ie.RowNumber = _currentRowNumber;
			ie.OffSet = _bos.getPosition();
			_indexBlock.add(ie);
			_nextBreak += _indexInterval;
		}

		for (int i = 0; i < _columnCount; i++) {
			Boolean isNull = false;
			if (data[i] == null) {
				isNull = true;
			}
			_writeMethods[i].Write(data[i], isNull);
		}
		_currentRowNumber++;
	}

	public DataStream getDataStream() throws IOException {
		this.close();
		if (_bos == null) {
			throw new IOException("No data written to either memory or file.");
		}

		if (_bos.IsFilestream()) {
			return new DataStream(_filename, _HeaderInformation);
		} else {
			return new DataStream(_bos.getBuffer(),_HeaderInformation);
		}
	}

	public byte[] getDataBuffer() throws IOException {
		if (_bos == null) {
			throw new IOException("No data written to either memory or file.");
		}

		if (_bos.IsFilestream()) {
			throw new IOException("getDataBuffer is only available when data is written to memory.");
		}

		return _bos.getBuffer();
	}

	public boolean isFilestream() {
		return _bos.IsFilestream();
	}

	public Map<DataFileEnums.BinaryFileInfo, Object> getHeader() {
		populateHeaderInformation();
		return _HeaderInformation;
	}

	protected void setupColumnWriters() throws IOException {
		int iCnt = _dataRow.getColumnCount();
		_writeMethods = new FieldReadWrite[iCnt];
		for (int i = 0; i < iCnt; i++) {
			_writeMethods[i] = getWriteMethod(_dataRow.getDataType(i));
			_dataRow.setDataType(i, adjustedDataType(_dataRow.getDataType(i)));
		}
		_columnCount = _dataRow.getColumnCount();
	}

	protected void writeInitialHeader() throws IOException {
		_bos.write(buildHeader());
	}

	protected void writeFinalHeader() throws IOException {
		_bos.writeFinalHeader(buildHeader());
	}

	protected byte[] buildHeader() throws IOException {
		byte[] aHeader;
		//@formatter:off
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); DataOutputStream dos = new DataOutputStream(baos)) {
			dos.writeByte(_byteFileType);          // (byte) Data file type 0=Data, 1=View
			dos.writeBoolean(_isEncrypted);        // (Boolean) Encrypted True/False
			dos.writeUTF(_fingerPrint);            // (string) The internal UUID used to identify this file.
			dos.writeUTF(_sourceDataFilename);     // (string) Write the name of the source dat file. '' if this is a dat file.
			dos.writeBoolean(_fullRowCountKnown);  // (boolean) Only used by DynamicSQL datasets - may or may not know full row count.
			dos.writeLong(_fullRowCount);          // (long) Row count of full record set
			dos.writeLong(_firstRow);              // (long) Row number of first data row in this file
			dos.writeLong(_lastRow);               // (long) Row number of last data row in this file
			dos.writeLong(_indexStart);            // (long) Offset to start of direct access index (Int64)
			dos.writeLong(_schemaStart);           // (long) Offset to start of Information block (Xml format)
			dos.writeLong(_dateCreated.getTime()); // (DateTime/long) Datetime file created
			dos.writeLong(_dateExpires.getTime()); // (DateTime/long) Datetime file expires
			baos.flush();
			aHeader = baos.toByteArray();
		}
		//@formatter:on
		return aHeader;
	}

	protected void writeFooter() throws IOException {
		// Write the index
		_indexStart = _bos.getPosition();
		for (IndexEntry indexEntry : _indexBlock) {
			long lRowNum = indexEntry.RowNumber;
			long lOffSet = indexEntry.OffSet;
			_bos.writeLong(lRowNum);
			_bos.writeLong(lOffSet);
		}

		// Write the InfoBlock
		_schemaStart = _bos.getPosition();
		Document xmlSchemaDoc = XmlUtilities.CreateXMLDocument("<FileInfo><DataInfo /></FileInfo>");
		if ((_dataRow != null) && (_dataRow.getColumnNames() != null)) {
			int columnCount = _dataRow.getColumnCount();
			for (int i = 0; i < columnCount; i++) {
				String columnName = _dataRow.getColumnName(i);
				Element eleCol = xmlSchemaDoc.createElement("DataColumn");
				eleCol.setAttribute("Name", columnName);
				eleCol.setAttribute("DataType", _dataRow.getDataType(i).toString());
				eleCol.setAttribute("ColumnType", _dataRow.getColumnType(i).toString());
				// eleCol.SetAttribute("GlobalValue",
				// _DataRow.aValues[i].ToString());
				// eleCol.SetAttribute("NullCount",
				// _aColumnDetails[i].NullCount.ToString());
				// eleCol.SetAttribute("MinValue", _aColumnDetails[i].MinValue);
				// eleCol.SetAttribute("MaxValue", _aColumnDetails[i].MaxValue);
				// eleCol.SetAttribute("MinLength",
				// _aColumnDetails[i].MinLength.ToString());
				// eleCol.SetAttribute("MaxLength",
				// _aColumnDetails[i].MaxLength.ToString());
				// eleCol.SetAttribute("Sum",
				// _aColumnDetails[i].Sum.ToString());
				// eleCol.SetAttribute("Average",
				// ComputeAverage(_aColumnDetails[i].Sum).ToString());

				if (_columProfiles.containsKey(columnName)) {
					List<String[]> columnProfiles = _columProfiles.get(columnName);
					for (String[] profile : columnProfiles) {
						Element eleProfile = xmlSchemaDoc.createElement(profile[0]);
						eleProfile.setAttribute("DataType", profile[1]);
						eleProfile.setAttribute("Value", profile[2]);
						eleCol.appendChild(eleProfile);
					}
				}
				xmlSchemaDoc.getDocumentElement().getFirstChild().appendChild(eleCol); // .documentElement.FirstChild.AppendChild(eleCol);
			}

			for (Map.Entry<String, String[]> kvp : _globalValues.entrySet()) {
				Element eleCol = xmlSchemaDoc.createElement("DataColumn");
				eleCol.setAttribute("Name", kvp.getKey());
				eleCol.setAttribute("DataType", kvp.getValue()[0]);
				eleCol.setAttribute("ColumnType", "GlobalValue");
				eleCol.setAttribute("GlobalValue", kvp.getValue()[1]);
				xmlSchemaDoc.getDocumentElement().getFirstChild().appendChild(eleCol); // .DocumentElement.FirstChild.AppendChild(eleCol);
			}
		}
		_schemaXML = XmlUtilities.XMLDocumentToString(xmlSchemaDoc);

		if (_isEncrypted) {
			_bos.writeUTF(CryptoUtilities.EncryptDecrypt(_schemaXML));
		} else {
			_bos.writeUTF(_schemaXML);
		}

		_dateCreated = new Date();
	}

	private DataType adjustedDataType(DataType ColumnDataType) {
		// Simplified code to convert some types into others. E.g. Byte, Int16,
		// SByte ==> Int32
		switch (ColumnDataType) {
		case BigDecimalData:
			return DataType.DoubleData;
		case ByteData:
			return DataType.IntegerData;
		case FloatData:
			return DataType.DoubleData;
		case ShortData:
			return DataType.IntegerData;
		case SqlTimestampData:
			return DataType.DateData;
		default:
			return ColumnDataType;
		}
	}

	private FieldReadWrite getWriteMethod(DataType ColumnDataType) throws IOException {
		// Simplified code to convert some types into others. E.g. Byte, Int16,
		// SByte ==> Int32
		switch (ColumnDataType) {
		case BigDecimalData:
			return new FieldBigDecimal(_bos);
		case BooleanData:
			return new FieldBoolean(_bos);
		case ByteData:
			return new FieldByte(_bos);
		case CharData:
			return new FieldChar(_bos);
		case DateData:
			return new FieldDate(_bos);
		case DoubleData:
			return new FieldDouble(_bos);
		case FloatData:
			return new FieldFloat(_bos);
		case IntegerData:
			return new FieldInteger(_bos);
		case LongData:
			return new FieldLong(_bos);
		case ShortData:
			return new FieldShort(_bos);
		case SqlTimestampData:
			return new FieldSqlTimestamp(_bos);
		case StringData:
			if (_isEncrypted) {
				return new FieldStringEncrypted(_bos);
			} else {
				return new FieldString(_bos);
			}
		case UUIDData:
			return new FieldUUID(_bos);
		case ObjectData:
			return new FieldObject(_bos);
		default:
			throw new IOException("Data type " + ColumnDataType.toString() + " is not currently supported by the data engine.");
		}
	}

}
