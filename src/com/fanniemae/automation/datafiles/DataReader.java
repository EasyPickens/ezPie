package com.fanniemae.automation.datafiles;

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

import com.fanniemae.automation.common.CryptoUtilities;
import com.fanniemae.automation.common.DataStream;
import com.fanniemae.automation.common.FileUtilities;
import com.fanniemae.automation.common.StringUtilities;
import com.fanniemae.automation.common.XmlUtilities;
import com.fanniemae.automation.datafiles.lowlevel.BinaryInputStream;
import com.fanniemae.automation.datafiles.lowlevel.DataFileEnums;
import com.fanniemae.automation.datafiles.lowlevel.DataFileEnums.ColumnTypes;
import com.fanniemae.automation.datafiles.lowlevel.DataFileEnums.DataType;
import com.fanniemae.automation.datafiles.lowlevel.DataFormat;
import com.fanniemae.automation.datafiles.lowlevel.DataRow;
import com.fanniemae.automation.datafiles.lowlevel.FieldByte;
import com.fanniemae.automation.datafiles.lowlevel.FieldChar;
import com.fanniemae.automation.datafiles.lowlevel.FieldDate;
import com.fanniemae.automation.datafiles.lowlevel.FieldDouble;
import com.fanniemae.automation.datafiles.lowlevel.FieldInteger;
import com.fanniemae.automation.datafiles.lowlevel.FieldLong;
import com.fanniemae.automation.datafiles.lowlevel.FieldReadWrite;
import com.fanniemae.automation.datafiles.lowlevel.FieldBoolean;
import com.fanniemae.automation.datafiles.lowlevel.FieldString;
import com.fanniemae.automation.datafiles.lowlevel.FieldStringEncrypted;
import com.fanniemae.automation.datafiles.lowlevel.FieldUUID;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-28
 * 
 */
public class DataReader extends DataFormat {
    private final BinaryInputStream _bis;
    private FieldReadWrite[] _ReadMethods;
    
    protected DataReader _drSourceData = null;
    protected long _EndOfDataBlock;

    public DataReader(DataStream ds) throws IOException {
        if (ds.IsMemory()) {
            _bis = new BinaryInputStream(ds.getMemorystream());
        } else {
            _bis = new BinaryInputStream(ds.getFilename());
        }
        initialize();
    }

    public DataReader(String filename) throws IOException {
        _bis = new BinaryInputStream(filename);
        initialize();
    }

    public DataReader(byte[] byteArrayMemoryStream) throws IOException {
        _bis = new BinaryInputStream(byteArrayMemoryStream);
        initialize();
    }

    @Override
    public void close() throws Exception {
        if ((_bis != null) && (!_bDisposed)) {
            _bis.close();
            _bDisposed = true;
        }
        if (_drSourceData != null) {
            _drSourceData.clone();
        }
    }

    public boolean eof() throws IOException {
        return _bis.getPosition() >= _EndOfDataBlock;
    }

    public DataRow getDataRowAndSchemaAt(long offSet) throws IOException {
        _bis.seek(offSet);
        return getDataRowAndSchema();
    }

    public DataRow getDataRowAndSchema() throws IOException {
        _DataRow.setValues(getDataRow());
        if (_byteFileType == 0) {
            return _DataRow;
        }
        return _drSourceData.getDataRowAndSchemaAt((long) _DataRow.getValues()[0]);
    }

    public void moveToRow(long rowNumber) throws IOException {
        rowNumber = (rowNumber <= 0L) ? 1L : rowNumber;

        _nCurrentRowNumber = _nFirstRow - 1L;
        if ((rowNumber > _nFullRowCount) || (rowNumber > _nLastRow)) {
            _bis.seek(_EndOfDataBlock);
            _nCurrentRowNumber = _nLastRow;
        } else if (rowNumber == 1L) {
            _bis.seek(_nStartOfData);
        } else if (_aIndex.size() > 0) {
            long lRowNum = _nFirstRow - 1L;
            long lPosition = _nStartOfData;
            int iLength = _aIndex.size();
            for (int i = 0; i < iLength; i++) {
                if (_aIndex.get(i).RowNumber + _nFirstRow - 1L < rowNumber) {
                    lRowNum = _aIndex.get(i).RowNumber + _nFirstRow - 1L;
                    lPosition = _aIndex.get(i).OffSet;
                } else {
                    break;
                }
            }
            _nCurrentRowNumber = lRowNum;
            _bis.seek(lPosition);
            while (!eof() && (_nCurrentRowNumber < rowNumber)) {
                skipDataRow();
            }
        } else {
            while (!eof() && (_nCurrentRowNumber < rowNumber)) {
                skipDataRow();
                //getRowValues();
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
        for (int i = 0; i < _DataRow.getColumnCount(); i++) {
            if (_DataRow.getColumnType(i) == ColumnTypes.DataValue) {
                _ReadMethods[i].Read();
            }
        }
        _nCurrentRowNumber++;
    }
    
    public Object[] getDataRowAt(long position) throws IOException {
        _bis.seek(position);
        return getDataRow();
    }

    public Object[] getDataRow() throws IOException {
    	int length = _DataRow.getColumnCount();
        Object[] aData = new Object[length];
        for (int i = 0; i < length; i++) {
            if (_DataRow.getColumnType(i) == ColumnTypes.GlobalValue) {
                aData[i] = _DataRow.getValue(i);
            } else {
                aData[i] = _ReadMethods[i].Read();
            }
        }
        _nCurrentRowNumber++;
        return aData;
    }

    public Object getHeaderInformation(DataFileEnums.BinaryFileInfo key) {
        return _aHeaderInformation.get(key);
    }

    public Map<DataFileEnums.BinaryFileInfo, Object> getHeader() {
        return _aHeaderInformation;
    }

    public String[] getColumnNames() {
        return _DataRow.getColumnNames();
    }

    public DataType[] getDataTypes() {
        return _DataRow.getDataTypes();
    }
    
    public long getFilePosition() throws IOException {
    	return _bis.getPosition();
    }

    protected void initialize() throws IOException {
        readHeader();
        int length = _DataRow.getColumnCount();
        _ReadMethods = new FieldReadWrite[length];
        for (int i = 0; i < _ReadMethods.length; i++) {
            _ReadMethods[i] = getReadMethod(_DataRow.getDataType(i));
        }
    }

    protected void readHeader() throws IOException {
        _byteFileType = _bis.readByte();
        _bEncrypted = _bis.readBoolean();
        _sFingerPrint = _bis.readUTF();
        _sSourceDataFilename = _bis.readUTF();
        _bFullRowCountKnown = _bis.readBoolean();
        _nFullRowCount = _bis.readLong();
        _nFirstRow = _bis.readLong();
        _nLastRow = _bis.readLong();
        _nIndexStart = _bis.readLong();
        _nSchemaStart = _bis.readLong();
        _dtCreated = new Date(_bis.readLong());
        _dtExpires = new Date(_bis.readLong());
        _EndOfDataBlock = _nIndexStart;
        _nStartOfData = _bis.getPosition();

        // Jump to the Index
        _bis.seek(_nIndexStart);
        _aIndex = new ArrayList<>();
        while (_bis.getPosition() < _nSchemaStart) {
            IndexEntry ie = new IndexEntry();
            ie.RowNumber = _bis.readLong();
            ie.OffSet = _bis.readLong();
            _aIndex.add(ie);
        }

        // Jump to Schema and load it.
        _bis.seek(_nSchemaStart);
        _sSchemaXML = _bis.readUTF();
        if (_bEncrypted) {
            _sSchemaXML = CryptoUtilities.EncryptDecrypt(_sSchemaXML);
        }

        PopulateHeaderInformation();

        try {
            Document doc = XmlUtilities.CreateXMLDocument(_sSchemaXML);
            XPathFactory xFactory = XPathFactory.newInstance();
            XPath xp = xFactory.newXPath();
            XPathExpression expr = xp.compile("FileInfo/DataInfo/DataColumn");

            NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            if (nodes.getLength() == 0) {
                throw new IOException("Data file does not contain any column information.");
            }

            _DataRow = new DataRow(nodes.getLength());
            for (int i = 0; i < nodes.getLength(); i++) {
                Element elementColumn = (Element) nodes.item(i);

                DataFileEnums.ColumnTypes eColType = DataFileEnums.ColumnTypes.DataValue;

                String name = elementColumn.getAttribute("Name");
                String dataType = elementColumn.getAttribute("DataType");
                String sGlobalValue = elementColumn.getAttribute("GlobalValue");
                Object globalValue = null;
                if (elementColumn.getAttribute("ColumnType").equalsIgnoreCase("GlobalValue")) {
                    eColType = DataFileEnums.ColumnTypes.GlobalValue;
                    globalValue = StringUtilities.toObject(dataType, sGlobalValue);
                }
                _DataRow.DefineColumn(i, name, eColType, dataType, globalValue);
            }
        } catch (XPathExpressionException | IOException ex) {
            throw new IOException("Error reading data file header. ", ex);
        }

        // If this is a view file, check for source data.
        if (_byteFileType == 1) {
            File fd = new File(_sFilename);
            File dir = fd.getParentFile();
            String dataFilename = dir.getParent();
            if (!dataFilename.endsWith(java.io.File.separator)) {
                dataFilename += java.io.File.separator;
            }
            dataFilename += _sSourceDataFilename;
            if (FileUtilities.isInvalidFile(dataFilename)) {
                throw new IOException(String.format("Could not find the %s data file.", dataFilename));
            }

            _drSourceData = new DataReader(dataFilename);
            // Check to be sure the finger print still matches.
            if (!_sFingerPrint.equals(_drSourceData.getFingerPrint())) {
                // This file must be recreated based on the new data.
            }
        }
        _bis.seek(_nStartOfData);
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
                if (_bEncrypted) {
                    return new FieldStringEncrypted(_bis);
                } else {
                    return new FieldString(_bis);
                }
            default:
                throw new IOException("Data type " + columnDataType.toString() + " is not currently supported by the data engine.");
        }
    }

}
