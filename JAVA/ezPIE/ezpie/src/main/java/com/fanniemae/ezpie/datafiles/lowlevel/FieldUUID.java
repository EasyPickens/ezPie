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

package com.fanniemae.ezpie.datafiles.lowlevel;

import java.io.IOException;
import java.util.UUID;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2015-12-28
 * 
 */

public class FieldUUID extends FieldReadWrite {

    public FieldUUID(BinaryInputStream streamIn) {
        super(streamIn);
    }

    public FieldUUID(BinaryOutputStream streamOut) {
        super(streamOut);
    }

    @Override
    public Object Read() throws IOException {
        String sUUID = _bis.readUTF();
        if (sUUID.equals("")) {
            return null;
        }
        return UUID.fromString(sUUID);
    }

    @Override
    public void Write(Object o, Boolean bIsNull) throws IOException {
        if (bIsNull) {
            _bos.writeUTF("");
            return;
        }
        _bos.writeUTF(((UUID) o).toString()); //Write UUID to string
    }

}
