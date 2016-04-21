package com.fanniemae.devtools.pie.datafiles.lowlevel;

import java.io.IOException;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-28
 * 
 */
public class FieldFloat extends FieldReadWrite {

    public FieldFloat(BinaryInputStream streamIn) {
        super(streamIn);
    }

    public FieldFloat(BinaryOutputStream streamOut) {
        super(streamOut);
    }

    @Override
    public Object Read() throws IOException {
        float f = _bis.readFloat();
        if (f == Float.MIN_VALUE) {
            return null;
        }
        return f;
    }

    @Override
    public void Write(Object o, Boolean bIsNull) throws IOException {
        if (bIsNull) {
            _bos.writeDouble(Double.MIN_VALUE);
            return;
        }
        _bos.writeDouble((double) ((float) o));
    }
}
