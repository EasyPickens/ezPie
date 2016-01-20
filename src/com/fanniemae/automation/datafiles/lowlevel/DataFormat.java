package com.fanniemae.automation.datafiles.lowlevel;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.fanniemae.automation.datafiles.lowlevel.DataFileEnums.ColumnTypes;
import com.fanniemae.automation.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-28
 * 
 */
abstract public class DataFormat implements AutoCloseable {
	protected String _Filename = "";
    protected DataRow _DataRow;
    protected ArrayList<IndexEntry> _IndexBlock = null;
    protected int _MemoryLimitInBytes = 20971520;   // = 20 megs or 10485760 = 10 megs;

    protected boolean _ProfileData = false;
    protected boolean _Disposed = false;
    protected long _IndexInterval = 5000L;       // Used by index to determine how often to add entry.
    protected long _NextBreak = 5000L;      // Next row count to add an index entry.
    protected long _CurrentRowNumber = 0L;
    protected long _StartOfDataBlock = 0L;

    // File header information in order
    protected byte _byteFileType = 0;
    protected boolean _isEncrypted = true;
    protected String _FingerPrint;
    protected String _SourceDataFilename = "";
    protected boolean _FullRowCountKnown = true;
    protected long _FullRowCount = 0L;
    protected long _FirstRow = 0L;
    protected long _LastRow = 0L;
    protected long _IndexStart = 0L;
    protected long _SchemaStart = 0L;
    protected Date _DateCreated = new Date();
    protected Date _DateExpires = new Date();
    protected String _SchemaXML = "";

    protected Map<DataFileEnums.BinaryFileInfo, Object> _HeaderInformation = null;

    public String getFingerPrint() {
        return _FingerPrint;
    }

    public long getFullRowCount() {
        return _FullRowCount;
    }

    public void setFullRowCount(long value) {
        _FullRowCount = value;
    }

    public long getBufferFirstRow() {
        return _FirstRow;
    }

    public void setBufferFirstRow(long value) {
        _FirstRow = value;
    }

    public long getBufferLastRow() {
        return _LastRow;
    }

    public void setBufferLastRow(long value) {
        _LastRow = value;
    }

    public Date getBufferExpires() {
        return _DateExpires;
    }

    public void setBufferExpires(Date value) {
        _DateExpires = value;
    }

    public Date getDateCreated() {
        return _DateCreated;
    }
    
    public void setFullRowCountKnown(Boolean value) {
        _FullRowCountKnown = value;
    }

    public DataFormat() {
        _IndexBlock = new ArrayList<>();
    }

    public void defineDataColumn(int index, String columnName, DataType columnDataType) {
        _DataRow.DefineColumn(index, columnName, columnDataType);
    }

    public void defineDataColumn(int index, String columnName, String javaDataType) {
        defineDataColumn(index, columnName, javaDataType, ColumnTypes.DataValue, "");
    }

    public void addGlobalValueColumn(int index, String columnName, String javaDataType, String globalValue) {
        defineDataColumn(index, columnName, javaDataType, ColumnTypes.GlobalValue, globalValue);
    }

    protected void defineDataColumn(int index, String columnName, String javaDataType, ColumnTypes columnType, String globalValue) {
        _DataRow.DefineColumn(index, columnName, columnType, javaDataType, null);
    }

    protected void populateHeaderInformation() {
        _HeaderInformation = new HashMap<>();
        _HeaderInformation.put(DataFileEnums.BinaryFileInfo.DateCreated, _DateCreated);
        _HeaderInformation.put(DataFileEnums.BinaryFileInfo.Encrypted, _isEncrypted);
        _HeaderInformation.put(DataFileEnums.BinaryFileInfo.DateExpires, _DateExpires);
        _HeaderInformation.put(DataFileEnums.BinaryFileInfo.FileType, _byteFileType);
        _HeaderInformation.put(DataFileEnums.BinaryFileInfo.FingerPrint, _FingerPrint);
        _HeaderInformation.put(DataFileEnums.BinaryFileInfo.BufferFirstRow, _FirstRow);
        _HeaderInformation.put(DataFileEnums.BinaryFileInfo.BufferLastRow, _LastRow);
        _HeaderInformation.put(DataFileEnums.BinaryFileInfo.FullRowCountKnown,_FullRowCountKnown);
        _HeaderInformation.put(DataFileEnums.BinaryFileInfo.RowCount, _FullRowCount);
        _HeaderInformation.put(DataFileEnums.BinaryFileInfo.DatFilename, _SourceDataFilename);
        _HeaderInformation.put(DataFileEnums.BinaryFileInfo.SchemaXML, _SchemaXML);
    }

    public class IndexEntry {
        public long RowNumber;
        public long OffSet;
    }

}
