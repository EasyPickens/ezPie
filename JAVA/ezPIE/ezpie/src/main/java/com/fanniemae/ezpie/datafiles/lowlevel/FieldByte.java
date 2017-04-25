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

public class FieldByte extends FieldReadWrite {

	public FieldByte(BinaryInputStream streamIn) {
		super(streamIn);
	}

	public FieldByte(BinaryOutputStream streamOut) {
		super(streamOut);
	}

	@Override
	public Object Read() throws IOException {
		return _bis.readByte();
	}

	@Override
	public void Write(Object o, Boolean bIsNull) throws IOException {
		if (bIsNull) {
			_bos.writeInt(Integer.MIN_VALUE);
			return;
		}
		_bos.writeInt((int) ((byte) o));
	}

}
