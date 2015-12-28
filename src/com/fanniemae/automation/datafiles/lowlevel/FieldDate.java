package com.fanniemae.automation.datafiles.lowlevel;

import java.io.IOException;
import java.util.Date;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-28
 * 
 */
public class FieldDate extends FieldReadWrite {

	public FieldDate(BinaryInputStream streamIn) {
		super(streamIn);
	}

	public FieldDate(BinaryOutputStream streamOut) {
		super(streamOut);
	}

	@Override
	public Object Read() throws IOException {
		long l = _bis.readLong();
		if (l == 0) {
			return null;
		}
		return new Date(l);
	}

	@Override
	public void Write(Object o, Boolean bIsNull) throws IOException {
		if (bIsNull) {
			_bos.writeLong(0);
			return;
		}
		_bos.writeLong(((Date) o).getTime());
	}
}
