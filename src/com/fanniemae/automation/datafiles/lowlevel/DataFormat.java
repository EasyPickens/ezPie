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
	protected String _sFilename = "";
    protected DataRow _DataRow;
    protected ArrayList<IndexEntry> _aIndex = null;
    protected int _nMemoryLimit = 20971520;   // = 20 megs or 10485760 = 10 megs;

    protected boolean _bProfileData = false;
    protected boolean _bDisposed = false;
    protected long _nInterval = 5000L;       // Used by index to determine how often to add entry.
    protected long _nNextBreak = 5000L;      // Next row count to add an index entry.
    protected long _nCurrentRowNumber = 0L;
    protected long _nStartOfData = 0L;

    // File header information in order
    protected byte _byteFileType = 0;
    protected boolean _bEncrypted = true;
    protected String _sFingerPrint;
    protected String _sSourceDataFilename = "";
    protected boolean _bFullRowCountKnown = true;
    protected long _nFullRowCount = 0L;
    protected long _nFirstRow = 0L;
    protected long _nLastRow = 0L;
    protected long _nIndexStart = 0L;
    protected long _nSchemaStart = 0L;
    protected Date _dtCreated = new Date();
    protected Date _dtExpires = new Date();
    protected String _sSchemaXML = "";

    protected Map<DataFileEnums.BinaryFileInfo, Object> _aHeaderInformation = null;

    public String getFingerPrint() {
        return _sFingerPrint;
    }

    public long getFullRowCount() {
        return _nFullRowCount;
    }

    public void setFullRowCount(long value) {
        _nFullRowCount = value;
    }

    public long getBufferFirstRow() {
        return _nFirstRow;
    }

    public void setBufferFirstRow(long value) {
        _nFirstRow = value;
    }

    public long getBufferLastRow() {
        return _nLastRow;
    }

    public void setBufferLastRow(long value) {
        _nLastRow = value;
    }

    public Date getBufferExpires() {
        return _dtExpires;
    }

    public void setBufferExpires(Date value) {
        _dtExpires = value;
    }

    public Date getDateCreated() {
        return _dtCreated;
    }
    
    public void setFullRowCountKnown(Boolean value) {
        _bFullRowCountKnown = value;
    }

    public DataFormat() {
        _aIndex = new ArrayList<>();
    }

    public void DefineDataColumn(int index, String columnName, DataType columnDataType) {
        _DataRow.DefineColumn(index, columnName, columnDataType);
    }

    public void DefineDataColumn(int index, String columnName, String javaDataType) {
        DefineDataColumn(index, columnName, javaDataType, ColumnTypes.DataValue, "");
    }

    public void AddGlobalValueColumn(int index, String columnName, String javaDataType, String globalValue) {
        DefineDataColumn(index, columnName, javaDataType, ColumnTypes.GlobalValue, globalValue);
    }

    protected void DefineDataColumn(int index, String columnName, String javaDataType, ColumnTypes columnType, String globalValue) {
        _DataRow.DefineColumn(index, columnName, columnType, javaDataType, null);
    }

    protected void PopulateHeaderInformation() {
        _aHeaderInformation = new HashMap<>();
        _aHeaderInformation.put(DataFileEnums.BinaryFileInfo.DateCreated, _dtCreated);
        _aHeaderInformation.put(DataFileEnums.BinaryFileInfo.Encrypted, _bEncrypted);
        _aHeaderInformation.put(DataFileEnums.BinaryFileInfo.DateExpires, _dtExpires);
        _aHeaderInformation.put(DataFileEnums.BinaryFileInfo.FileType, _byteFileType);
        _aHeaderInformation.put(DataFileEnums.BinaryFileInfo.FingerPrint, _sFingerPrint);
        _aHeaderInformation.put(DataFileEnums.BinaryFileInfo.BufferFirstRow, _nFirstRow);
        _aHeaderInformation.put(DataFileEnums.BinaryFileInfo.BufferLastRow, _nLastRow);
        _aHeaderInformation.put(DataFileEnums.BinaryFileInfo.FullRowCountKnown,_bFullRowCountKnown);
        _aHeaderInformation.put(DataFileEnums.BinaryFileInfo.RowCount, _nFullRowCount);
        _aHeaderInformation.put(DataFileEnums.BinaryFileInfo.DatFilename, _sSourceDataFilename);
        _aHeaderInformation.put(DataFileEnums.BinaryFileInfo.SchemaXML, _sSchemaXML);
    }

    public class IndexEntry {
        public long RowNumber;
        public long OffSet;
    }

}
