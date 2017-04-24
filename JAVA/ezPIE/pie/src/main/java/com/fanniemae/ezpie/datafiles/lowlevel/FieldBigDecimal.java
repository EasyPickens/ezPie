/**
 *  
 * Copyright (c) 2015 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPIE/wiki/Fannie-Mae-Open-Source-Licensing-Project
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.datafiles.lowlevel;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
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
