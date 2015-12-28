package com.fanniemae.automation.datafiles.lowlevel;

import java.io.IOException;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-28
 * 
 */
public class FieldDouble extends FieldReadWrite {

	public FieldDouble(BinaryInputStream streamIn) {
		super(streamIn);
	}

	public FieldDouble(BinaryOutputStream streamOut) {
		super(streamOut);
	}

	@Override
	public Object Read() throws IOException {
		double d = _bis.readDouble();
		if (d == Double.MIN_VALUE) {
			return null;
		}
		return d;
	}

	@Override
	public void Write(Object o, Boolean bIsNull) throws IOException {
		if (bIsNull) {
			_bos.writeDouble(Double.MIN_VALUE);
			return;
		}
		_bos.writeDouble((double) o);
	}

}
