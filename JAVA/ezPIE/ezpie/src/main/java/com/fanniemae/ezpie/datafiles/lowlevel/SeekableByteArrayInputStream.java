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

import java.io.ByteArrayInputStream;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2015-12-28
 * 
 */

public class SeekableByteArrayInputStream extends ByteArrayInputStream {
	public SeekableByteArrayInputStream(byte[] buf) {
		super(buf);
	}

	public SeekableByteArrayInputStream(byte[] buf, int offset, int length) {
		super(buf, offset, length);
	}

	public void seek(int pos) {
		this.pos = pos;
	}

	public int getPosition() {
		return this.pos;
	}
}
