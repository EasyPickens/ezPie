package com.fanniemae.devtools.pie.datafiles.lowlevel;

import java.io.IOException;

/**
 * 
 * @author Richard Monson
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
