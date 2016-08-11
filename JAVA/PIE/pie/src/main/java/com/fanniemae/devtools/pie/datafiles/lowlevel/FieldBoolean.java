package com.fanniemae.devtools.pie.datafiles.lowlevel;

import java.io.IOException;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-28
 * 
 */
public class FieldBoolean extends FieldReadWrite {

	public FieldBoolean(BinaryInputStream streamIn) {
		super(streamIn);
	}

	public FieldBoolean(BinaryOutputStream streamOut) {
		super(streamOut);
	}

	@Override
	public Object Read() throws IOException {
		return _bis.readBoolean();
	}

	@Override
	public void Write(Object o, Boolean bIsNull) throws IOException {
		if (bIsNull) {
			_bos.writeBoolean(false);
			return;
		}
		_bos.writeBoolean((Boolean) o);
	}

}
