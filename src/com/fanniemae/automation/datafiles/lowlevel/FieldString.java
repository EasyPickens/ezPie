package com.fanniemae.automation.datafiles.lowlevel;

import java.io.IOException;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-28
 * 
 */
public class FieldString extends FieldReadWrite {

    public FieldString(BinaryInputStream streamIn) {
        super(streamIn);
    }

    public FieldString(BinaryOutputStream streamOut) {
        super(streamOut);
    }

    @Override
    public Object Read() throws IOException {
        if (_bis.readBoolean()) {
            return null;
        }
        return _bis.readUTF();
    }

    @Override
    public void Write(Object o, Boolean bIsNull) throws IOException {
        if (bIsNull) {
            _bos.writeBoolean(true);
            return;
        }
        _bos.writeBoolean(false);
        _bos.writeUTF((String) o);
    }
}
