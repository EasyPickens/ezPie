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

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2015-12-28
 * 
 */

public class FieldObject extends FieldReadWrite {

	public FieldObject(BinaryInputStream streamIn) {
		super(streamIn);
	}

	public FieldObject(BinaryOutputStream streamOut) {
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
        _bos.writeUTF(o.toString());
	}

}
