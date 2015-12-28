package com.fanniemae.automation.datafiles.lowlevel;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-28
 * 
 */
public class FieldBigDecimal extends FieldReadWrite {

    public FieldBigDecimal(BinaryInputStream streamIn) {
        super(streamIn);
    }

    public FieldBigDecimal(BinaryOutputStream streamOut) {
        super(streamOut);
    }

    @Override
    public Object Read() throws IOException {
        double d = _bis.readDouble();
        if (d == Double.MIN_VALUE) {
            return null;
        }
        return new BigDecimal(d);
    }

    @Override
    public void Write(Object o, Boolean bIsNull) throws IOException {
        if (bIsNull) {
            _bos.writeDouble(Double.MIN_VALUE);
            return;
        }
        BigDecimal bd = (BigDecimal) o;
        _bos.writeDouble(bd.doubleValue());
    }
}
