package com.fanniemae.automation.datafiles;

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

import com.fanniemae.automation.common.CryptoUtilities;
import com.fanniemae.automation.common.DataStream;
import com.fanniemae.automation.common.StringUtilities;
import com.fanniemae.automation.common.XmlUtilities;
import com.fanniemae.automation.datafiles.lowlevel.BinaryOutputStream;
import com.fanniemae.automation.datafiles.lowlevel.DataFileEnums;
import com.fanniemae.automation.datafiles.lowlevel.DataFileEnums.DataType;
import com.fanniemae.automation.datafiles.lowlevel.DataFormat;
import com.fanniemae.automation.datafiles.lowlevel.DataRow;
import com.fanniemae.automation.datafiles.lowlevel.FieldBigDecimal;
import com.fanniemae.automation.datafiles.lowlevel.FieldBoolean;
import com.fanniemae.automation.datafiles.lowlevel.FieldByte;
import com.fanniemae.automation.datafiles.lowlevel.FieldChar;
import com.fanniemae.automation.datafiles.lowlevel.FieldDate;
import com.fanniemae.automation.datafiles.lowlevel.FieldDouble;
import com.fanniemae.automation.datafiles.lowlevel.FieldFloat;
import com.fanniemae.automation.datafiles.lowlevel.FieldInteger;
import com.fanniemae.automation.datafiles.lowlevel.FieldLong;
import com.fanniemae.automation.datafiles.lowlevel.FieldReadWrite;
import com.fanniemae.automation.datafiles.lowlevel.FieldShort;
import com.fanniemae.automation.datafiles.lowlevel.FieldSqlTimestamp;
import com.fanniemae.automation.datafiles.lowlevel.FieldString;
import com.fanniemae.automation.datafiles.lowlevel.FieldStringEncrypted;
import com.fanniemae.automation.datafiles.lowlevel.FieldUUID;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-28
 * 
 */
public class DataWriter extends DataFormat {
	private final BinaryOutputStream _bos;
	protected FieldReadWrite[] _aWriteMethods = null;

	protected int _iColumnCount = 0;
	protected Map<String, String[]> _aGlobalValues = new HashMap<>();
	protected Map<String, List<String[]>> _aColumProfiles = new HashMap<>();

	public DataWriter(String sFilename) throws IOException {
		this(sFilename, 20, "", null, false);
	}

	public DataWriter(String sFilename, boolean ActiveSqlBuffer) throws IOException {
		this(sFilename, 0, "", null, ActiveSqlBuffer);
	}

	public DataWriter(String sFilename, int nMemoryLimitMegabytes) throws IOException {
		this(sFilename, nMemoryLimitMegabytes, "", null, false);
	}

	public DataWriter(String sFilename, int nMemoryLimitMegabytes, boolean ActiveSqlBuffer) throws IOException {
		this(sFilename, nMemoryLimitMegabytes, "", null, ActiveSqlBuffer);
	}

	public DataWriter(String sFilename, int nMemoryLimitMegabytes, String SourceDataFilename, UUID FingerPrint, boolean ActiveSqlBuffer) throws IOException {
		_sFilename = sFilename;
		_sSourceDataFilename = SourceDataFilename;
		_byteFileType = 1;

		if (ActiveSqlBuffer) {
			_nInterval = 500L; // Used by index to determine how often to add
								// entry.
			_nNextBreak = 500L; // Next row count to add an index entry.
		}

		if (FingerPrint == null) {
			_byteFileType = 0;
			_sFingerPrint = UUID.randomUUID().toString();
		} else {
			_sFingerPrint = FingerPrint.toString();
		}

		// If this is a View file, then get the dat filename only.
		if (StringUtilities.isNotNullOrEmpty(_sSourceDataFilename) && _sSourceDataFilename.contains(File.separator)) {
			File oFile = new File(_sSourceDataFilename);
			_sSourceDataFilename = oFile.getName();
		}

		_bos = new BinaryOutputStream(_sFilename, nMemoryLimitMegabytes, _sFingerPrint);
		WriteInitialHeader(); // Place holder for final information.
	}

	@Override
	public void close() throws IOException {
		if ((_bos != null) && (!_bDisposed)) {
			try {
				WriteFooter();
				WriteFinalHeader();
			} finally {
				_bos.close();
				_bDisposed = true;
			}
		}
	}

	public void SetupDataColumns(String[] aColumnNames, DataType[] aDataTypes) throws IOException {
		_DataRow = new DataRow(aColumnNames.length);
		for (int i = 0; i < aColumnNames.length; i++) {
			DefineDataColumn(i, aColumnNames[i], aDataTypes[i]);
		}
		SetupColumnWriters();
	}

	public void SetupDataColumns(String[][] ColumnNames_Types) throws IOException {
		int nColumnCount = ColumnNames_Types.length;
		_DataRow = new DataRow(nColumnCount);

		for (int i = 0; i < nColumnCount; i++) {
			DefineDataColumn(i, ColumnNames_Types[i][0], ColumnNames_Types[i][1]);
		}
		SetupColumnWriters();
	}

	public void UpdateGlobalValue(String sColumnName, String sDataType, String sValue) {
		_aGlobalValues.put(sColumnName, new String[] { sDataType, sValue });
	}

	public void UpdateColumnProfile(String sColumnName, List<String[]> aProfile) {
		_aColumProfiles.put(sColumnName, aProfile);
	}

	public void WriteDataRow(Object[] aData) throws IOException {
		if (_nCurrentRowNumber == _nNextBreak) {
			IndexEntry ie = new IndexEntry();
			ie.RowNumber = _nCurrentRowNumber;
			ie.OffSet = _bos.getPosition();
			_aIndex.add(ie);
			_nNextBreak += _nInterval;
		}

		for (int iCol = 0; iCol < _iColumnCount; iCol++) {
			Boolean bIsNull = false;
			if (aData[iCol] == null) {
				bIsNull = true;
			}
			_aWriteMethods[iCol].Write(aData[iCol], bIsNull);
		}
		_nCurrentRowNumber++;
	}

	public DataStream getDataStream() throws IOException {
		this.close();
		if (_bos == null) {
			throw new IOException("No data written to either memory or file.");
		}

		if (_bos.IsFilestream()) {
			return new DataStream(_sFilename);
		} else {
			return new DataStream(_bos.getBuffer());
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

	public boolean IsFilestream() {
		return _bos.IsFilestream();
	}

	public Map<DataFileEnums.BinaryFileInfo, Object> getHeader() {
		PopulateHeaderInformation();
		return _aHeaderInformation;
	}

	protected void SetupColumnWriters() throws IOException {
		int iCnt = _DataRow.getColumnCount();
		_aWriteMethods = new FieldReadWrite[iCnt];
		for (int i = 0; i < iCnt; i++) {
			_aWriteMethods[i] = GetWriteMethod(_DataRow.getDataType(i));
			_DataRow.setDataType(i, AdjustedDataType(_DataRow.getDataType(i)));
		}
		_iColumnCount = _DataRow.getColumnCount();
	}

	protected void WriteInitialHeader() throws IOException {
		_bos.write(BuildHeader());
	}

	protected void WriteFinalHeader() throws IOException {
		_bos.writeFinalHeader(BuildHeader());
	}

	protected byte[] BuildHeader() throws IOException {
		byte[] aHeader;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); DataOutputStream dos = new DataOutputStream(baos)) {
			dos.writeByte(_byteFileType); // (byte) Data file type 0=Data,
											// 1=View
			dos.writeBoolean(_bEncrypted); // (Boolean) Encrypted True/False
			dos.writeUTF(_sFingerPrint); // (string) The internal UUID used to
											// identify this file.
			dos.writeUTF(_sSourceDataFilename); // (string) Write the name of
												// the source dat file. '' if
												// this is a dat file.
			dos.writeBoolean(_bFullRowCountKnown); // (boolean) Only used by
													// ActiveSQL connections -
													// may or may not know row
													// count.
			dos.writeLong(_nFullRowCount); // (long) Row count of full record
											// set
			dos.writeLong(_nFirstRow); // (long) Row number of first data row in
										// this file
			dos.writeLong(_nLastRow); // (long) Row number of last data row in
										// this file
			dos.writeLong(_nIndexStart); // (long) Offset to start of direct
											// access index (Int64)
			dos.writeLong(_nSchemaStart); // (long) Offset to start of
											// Information block (Xml format)
			dos.writeLong(_dtCreated.getTime()); // (DateTime/long) Datetime
													// file created
			dos.writeLong(_dtExpires.getTime()); // (DateTime/long) Datetime
													// file expires
			baos.flush();
			aHeader = baos.toByteArray();
		}
		return aHeader;
	}

	protected void WriteFooter() throws IOException {
		// Write the index
		_nIndexStart = _bos.getPosition();
		for (IndexEntry _aIndex1 : _aIndex) {
			long lRowNum = _aIndex1.RowNumber;
			long lOffSet = _aIndex1.OffSet;
			_bos.writeLong(lRowNum);
			_bos.writeLong(lOffSet);
		}

		// Write the InfoBlock
		_nSchemaStart = _bos.getPosition();
		Document xDoc = XmlUtilities.CreateXMLDocument("<FileInfo><DataInfo /></FileInfo>");
		if ((_DataRow != null) && (_DataRow.getColumnNames() != null)) {
			int iLen = _DataRow.getColumnCount();
			for (int i = 0; i < iLen; i++) {
				String sColumnName = _DataRow.getColumnName(i);
				Element eleCol = xDoc.createElement("DataColumn");
				eleCol.setAttribute("Name", sColumnName);
				eleCol.setAttribute("DataType", _DataRow.getDataType(i).toString());
				eleCol.setAttribute("ColumnType", _DataRow.getColumnType(i).toString());
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

				if (_aColumProfiles.containsKey(sColumnName)) {
					List<String[]> aProfile = _aColumProfiles.get(sColumnName);
					for (String[] aProfile1 : aProfile) {
						Element eleProfile = xDoc.createElement(aProfile1[0]);
						eleProfile.setAttribute("DataType", aProfile1[1]);
						eleProfile.setAttribute("Value", aProfile1[2]);
						eleCol.appendChild(eleProfile);
					}
				}
				xDoc.getDocumentElement().getFirstChild().appendChild(eleCol); // .documentElement.FirstChild.AppendChild(eleCol);
			}

			for (Map.Entry<String, String[]> kvp : _aGlobalValues.entrySet()) {
				Element eleCol = xDoc.createElement("DataColumn");
				eleCol.setAttribute("Name", kvp.getKey());
				eleCol.setAttribute("DataType", kvp.getValue()[0]);
				eleCol.setAttribute("ColumnType", "GlobalValue");
				eleCol.setAttribute("GlobalValue", kvp.getValue()[1]);
				xDoc.getDocumentElement().getFirstChild().appendChild(eleCol); // .DocumentElement.FirstChild.AppendChild(eleCol);
			}
		}
		_sSchemaXML = XmlUtilities.XMLDocumentToString(xDoc);

		if (_bEncrypted) {
			_bos.writeUTF(CryptoUtilities.EncryptDecrypt(_sSchemaXML));
		} else {
			_bos.writeUTF(_sSchemaXML);
		}

		_dtCreated = new Date();
	}

	private DataType AdjustedDataType(DataType ColumnDataType) {
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

	private FieldReadWrite GetWriteMethod(DataType ColumnDataType) throws IOException {
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
			if (_bEncrypted) {
				return new FieldStringEncrypted(_bos);
			} else {
				return new FieldString(_bos);
			}
		case UUIDData:
			return new FieldUUID(_bos);
		default:
			throw new IOException("Data type " + ColumnDataType.toString() + " is not currently supported by the data engine.");
		}
	}

}
