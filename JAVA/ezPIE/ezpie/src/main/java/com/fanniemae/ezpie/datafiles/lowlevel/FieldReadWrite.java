/**
 *  
 * Copyright (c) 2015 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
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

abstract public class FieldReadWrite {
	protected BinaryInputStream _bis;
	protected BinaryOutputStream _bos;

	public FieldReadWrite(BinaryInputStream streamIn) {
		_bis = streamIn;
	}

	public FieldReadWrite(BinaryOutputStream streamOut) {
		_bos = streamOut;
	}

	public abstract Object Read() throws IOException;

	public abstract void Write(Object o, Boolean bIsNull) throws IOException;
}
