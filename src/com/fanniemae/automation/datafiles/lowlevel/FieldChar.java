package com.fanniemae.automation.datafiles.lowlevel;

import java.io.IOException;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-28
 * 
 */
public class FieldChar extends FieldReadWrite {

    public FieldChar(BinaryInputStream streamIn) {
        super(streamIn);
    }

    public FieldChar(BinaryOutputStream streamOut) {
        super(streamOut);
    }

    @Override
    public Object Read() throws IOException {
        return _bis.readChar();
    }

    @Override
    public void Write(Object o, Boolean bIsNull) throws IOException {
        if (bIsNull) {
            _bos.writeChar(0);
            return;
        }
        _bos.writeChar((char) o);
    }
}
