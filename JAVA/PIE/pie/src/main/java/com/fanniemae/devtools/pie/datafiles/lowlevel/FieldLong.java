package com.fanniemae.devtools.pie.datafiles.lowlevel;

import java.io.IOException;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-28
 * 
 */
public class FieldLong extends FieldReadWrite {

    public FieldLong(BinaryInputStream streamIn) {
        super(streamIn);
    }

    public FieldLong(BinaryOutputStream streamOut) {
        super(streamOut);
    }

    @Override
    public Object Read() throws IOException {
        long l = _bis.readLong();
        if (l == Long.MIN_VALUE) {
            return null;
        }
        return l;
    }

    @Override
    public void Write(Object o, Boolean bIsNull) throws IOException {
        if (bIsNull) {
            _bos.writeLong(Long.MIN_VALUE);
            return;
        }
        _bos.writeLong((long) o);
    }

}
