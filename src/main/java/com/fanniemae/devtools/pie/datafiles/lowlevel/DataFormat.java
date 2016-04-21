package com.fanniemae.devtools.pie.datafiles.lowlevel;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.fanniemae.devtools.pie.datafiles.lowlevel.DataFileEnums.ColumnTypes;
import com.fanniemae.devtools.pie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-28
 * 
 */
abstract public class DataFormat implements AutoCloseable {
	protected String _filename = "";
    protected DataRow _dataRow;
    protected ArrayList<IndexEntry> _indexBlock = null;
    protected int _memoryLimitInBytes = 20971520;   // = 20 megs or 10485760 = 10 megs;

    protected boolean _profileData = false;
    protected boolean _disposed = false;
    protected long _indexInterval = 5000L;       // Used by index to determine how often to add entry.
    protected long _nextBreak = 5000L;      // Next row count to add an index entry.
    protected long _currentRowNumber = 0L;
    protected long _startOfDataBlock = 0L;

    // File header information in order
    protected byte _byteFileType = 0;
    protected boolean _isEncrypted = true;
    protected String _fingerPrint;
    protected String _sourceDataFilename = "";
    protected boolean _fullRowCountKnown = true;
    protected long _fullRowCount = 0L;
    protected long _firstRow = 0L;
    protected long _lastRow = 0L;
    protected long _indexStart = 0L;
    protected long _schemaStart = 0L;
    protected Date _dateCreated = new Date();
    protected Date _dateExpires = new Date();
    protected String _schemaXML = "";

    protected Map<DataFileEnums.BinaryFileInfo, Object> _HeaderInformation = null;

    public String getFingerPrint() {
        return _fingerPrint;
    }

    public long getFullRowCount() {
        return _fullRowCount;
    }

    public void setFullRowCount(long value) {
        _fullRowCount = value;
    }

    public long getBufferFirstRow() {
        return _firstRow;
    }

    public void setBufferFirstRow(long value) {
        _firstRow = value;
    }

    public long getBufferLastRow() {
        return _lastRow;
    }

    public void setBufferLastRow(long value) {
        _lastRow = value;
    }

    public Date getBufferExpires() {
        return _dateExpires;
    }

    public void setBufferExpires(Date value) {
        _dateExpires = value;
    }

    public Date getDateCreated() {
        return _dateCreated;
    }
    
    public void setFullRowCountKnown(Boolean value) {
        _fullRowCountKnown = value;
    }

    public DataFormat() {
        _indexBlock = new ArrayList<>();
    }

    public void defineDataColumn(int index, String columnName, DataType columnDataType) {
        _dataRow.DefineColumn(index, columnName, columnDataType);
    }

    public void defineDataColumn(int index, String columnName, String javaDataType) {
        defineDataColumn(index, columnName, javaDataType, ColumnTypes.DataValue, "");
    }

    public void addGlobalValueColumn(int index, String columnName, String javaDataType, String globalValue) {
        defineDataColumn(index, columnName, javaDataType, ColumnTypes.GlobalValue, globalValue);
    }

    protected void defineDataColumn(int index, String columnName, String javaDataType, ColumnTypes columnType, String globalValue) {
        _dataRow.DefineColumn(index, columnName, columnType, javaDataType, null);
    }

    protected void populateHeaderInformation() {
        _HeaderInformation = new HashMap<>();
        _HeaderInformation.put(DataFileEnums.BinaryFileInfo.DateCreated, _dateCreated);
        _HeaderInformation.put(DataFileEnums.BinaryFileInfo.Encrypted, _isEncrypted);
        _HeaderInformation.put(DataFileEnums.BinaryFileInfo.DateExpires, _dateExpires);
        _HeaderInformation.put(DataFileEnums.BinaryFileInfo.FileType, _byteFileType);
        _HeaderInformation.put(DataFileEnums.BinaryFileInfo.FingerPrint, _fingerPrint);
        _HeaderInformation.put(DataFileEnums.BinaryFileInfo.BufferFirstRow, _firstRow);
        _HeaderInformation.put(DataFileEnums.BinaryFileInfo.BufferLastRow, _lastRow);
        _HeaderInformation.put(DataFileEnums.BinaryFileInfo.FullRowCountKnown,_fullRowCountKnown);
        _HeaderInformation.put(DataFileEnums.BinaryFileInfo.RowCount, _fullRowCount);
        _HeaderInformation.put(DataFileEnums.BinaryFileInfo.DatFilename, _sourceDataFilename);
        _HeaderInformation.put(DataFileEnums.BinaryFileInfo.SchemaXML, _schemaXML);
    }

    public class IndexEntry {
        public long RowNumber;
        public long OffSet;
    }

}
