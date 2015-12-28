package com.fanniemae.automation.datafiles.lowlevel;

import java.io.IOException;

/**
 * 
 * @author Richard Monson
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
