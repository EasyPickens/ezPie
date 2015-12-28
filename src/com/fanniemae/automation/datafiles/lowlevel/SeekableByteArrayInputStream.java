package com.fanniemae.automation.datafiles.lowlevel;

import java.io.ByteArrayInputStream;

/**
 * 
 * @author Richard Monson
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
