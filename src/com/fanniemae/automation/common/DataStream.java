package com.fanniemae.automation.common;

import java.io.File;

public class DataStream {
    protected boolean _isMemory = true;
    protected byte[] _data;
    protected String _filename;
    
    public DataStream(byte[] data) {
        _data = data;
        _isMemory = true;
    }
    
    public DataStream(String Filename) {
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
}
