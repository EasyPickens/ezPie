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

package com.fanniemae.ezpie.common;

import java.io.File;
import java.util.Map;

import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2015-12-28
 * 
 */

public class DataStream {
    protected boolean _isMemory = true;
    protected byte[] _data;
    protected String _filename;
    protected String[][] _schema;
    protected Map<DataFileEnums.BinaryFileInfo, Object> _headerInformation;
    
    public DataStream(byte[] data, Map<DataFileEnums.BinaryFileInfo, Object> headerInformation, String[][] schema) {
        _data = data;
        _isMemory = true;
        _headerInformation = headerInformation;
        _schema = schema;
    }
    
    public DataStream(String Filename, Map<DataFileEnums.BinaryFileInfo, Object> headerInformation, String[][] schema) {
        _filename = Filename;
        _isMemory = false;
        _headerInformation = headerInformation;
        _schema = schema;
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
    
    public String[][] getSchema() {
    	return _schema;
    }
}
