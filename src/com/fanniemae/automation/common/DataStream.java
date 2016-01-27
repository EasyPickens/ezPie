package com.fanniemae.automation.common;

import java.io.File;
import java.util.Map;

import com.fanniemae.automation.datafiles.lowlevel.DataFileEnums;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-29
 * 
 */
public class DataStream {
    protected boolean _isMemory = true;
    protected byte[] _data;
    protected String _filename;
    protected Map<DataFileEnums.BinaryFileInfo, Object> _headerInformation;
    
    public DataStream(byte[] data, Map<DataFileEnums.BinaryFileInfo, Object> headerInformation) {
        _data = data;
        _isMemory = true;
    }
    
    public DataStream(String Filename, Map<DataFileEnums.BinaryFileInfo, Object> headerInformation) {
        _filename = Filename;
        _isMemory = false;
    }
    
    public boolean IsMemory() {
        return _isMemory;
    }
    
    public byte[] getMemorystream() {
        return _data;
    }
    
    public String getFilename() {
        return _filename;
    }
    
    public long getSize() {
    	if (_isMemory) {
    		return _data.length;
    	} else {
    		File fi = new File(_filename);
    		return fi.length();
    	}
    }
    
    public Map<DataFileEnums.BinaryFileInfo, Object> getHeader() {
    	return _headerInformation;
    }
}
