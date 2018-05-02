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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.fanniemae.ezpie.common.ExceptionUtilities;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2015-12-28
 * 
 */

public class BinaryInputStream implements AutoCloseable {
	protected boolean _isFilestream = false;
	protected String _filename = "";

	protected RandomAccessFile _raf = null;
	protected SeekableByteArrayInputStream _bais = null;
	protected FileInputStream _fis = null;
	protected BufferedInputStream _bis = null;
	protected SeekableDataInputStream _dis = null;

	protected long _length = 0;

	public BinaryInputStream(String Filename) throws FileNotFoundException, IOException {
		_filename = Filename;
		File fd = new File(_filename);
		if (!fd.exists()) {
			throw new FileNotFoundException(_filename + " file was not found.");
		}
		_length = fd.length();

		_raf = new RandomAccessFile(_filename, "r");
		_fis = new FileInputStream(_raf.getFD());
		_bis = new BufferedInputStream(_fis);
		_dis = new SeekableDataInputStream(_bis);
		_isFilestream = true;
	}

	public BinaryInputStream(byte[] aBuffer) throws IOException {
		if (aBuffer == null) {
			throw new IOException("No input buffer defined.");
		}
		_length = aBuffer.length;
		_bais = new SeekableByteArrayInputStream(aBuffer);
		_dis = new SeekableDataInputStream(_bais);
	}

	public void seek(long pos) throws IOException {
		if (_isFilestream) {
			_raf.seek(pos);
			_bis = new BufferedInputStream(_fis);
			_dis = new SeekableDataInputStream(_bis, pos);
		} else {
			if (pos < Integer.MIN_VALUE || pos > Integer.MAX_VALUE) {
				throw new IllegalArgumentException(pos + " cannot be cast to int without changing its value.");
			}
			_bais.seek((int) pos);
			_dis = new SeekableDataInputStream(_bais, pos);
		}
	}

	public long getPosition() throws IOException {
		return _dis.getPosition();
	}

	@Override
	public void close() throws IOException {
		try {
			if (_dis != null) {
				_dis.close();
			}
		} catch (IOException ex) {
			ExceptionUtilities.goSilent(ex);
		}
		try {
			if (_bis != null) {
				_bis.close();
			}
		} catch (IOException ex) {
			ExceptionUtilities.goSilent(ex);
		}
		try {
			if (_fis != null) {
				_fis.close();
			}
		} catch (IOException ex) {
			ExceptionUtilities.goSilent(ex);
		}
		try {
			if (_raf != null) {
				_raf.close();
			}
		} catch (IOException ex) {
			ExceptionUtilities.goSilent(ex);
		}
		try {
			if (_bais != null) {
				_bais.close();
			}
		} catch (IOException ex) {
			ExceptionUtilities.goSilent(ex);
		}
	}

	public int read(byte[] b) throws IOException {
		return _dis.read(b);
	}

	public int read(byte[] b, int off, int len) throws IOException {
		return _dis.read(b, off, len);
	}

	public boolean readBoolean() throws IOException {
		return _dis.readBoolean();
	}

	public byte readByte() throws IOException {
		return _dis.readByte();
	}

	public char readChar() throws IOException {
		return _dis.readChar();
	}

	public double readDouble() throws IOException {
		return _dis.readDouble();
	}

	public float readFloat() throws IOException {
		return _dis.readFloat();
	}

	public void readFully(byte[] b) throws IOException {
		_dis.readFully(b);
	}

	public void readFully(byte[] b, int off, int len) throws IOException {
		_dis.readFully(b, off, len);
	}

	public int readInt() throws IOException {
		return _dis.readInt();
	}

	public long readLong() throws IOException {
		return _dis.readLong();
	}

	public short readShort() throws IOException {
		return _dis.readShort();
	}

	public int readUnsignedByte() throws IOException {
		return _dis.readUnsignedByte();
	}

	public int readUnsignedShort() throws IOException {
		return _dis.readUnsignedShort();
	}

	public String readUTF() throws IOException {
		return _dis.readUTF();
	}
}
