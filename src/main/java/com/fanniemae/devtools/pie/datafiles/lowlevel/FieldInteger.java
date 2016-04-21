package com.fanniemae.devtools.pie.datafiles.lowlevel;

import java.io.IOException;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-28
 * 
 */
public class FieldInteger extends FieldReadWrite {

    public FieldInteger(BinaryInputStream streamIn) {
        super(streamIn);
    }

    public FieldInteger(BinaryOutputStream streamOut) {
        super(streamOut);
    }

    @Override
    public Object Read() throws IOException {
        int i = _bis.readInt();
        if (i == Integer.MIN_VALUE) {
            return null;
        }
        return i;
    }

    @Override
    public void Write(Object o, Boolean bIsNull) throws IOException {
        if (bIsNull) {
            _bos.writeInt(Integer.MIN_VALUE);
            return;
        }
        _bos.writeInt((int) o);
    }

}
