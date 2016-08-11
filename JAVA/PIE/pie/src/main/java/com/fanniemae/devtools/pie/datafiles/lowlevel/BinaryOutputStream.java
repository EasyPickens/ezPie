package com.fanniemae.devtools.pie.datafiles.lowlevel;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-28
 * 
 */
public class BinaryOutputStream implements AutoCloseable {
	protected Boolean _monitorMemory = false;
    protected Boolean _isFilestream = false;
    protected Boolean _isOpen = false;

    protected String _filename = "";
    protected long _memoryLimit = 20971520;  //10485760;
    protected String _fingerPrint;

    protected DataOutputStream _dos = null;
    protected FileOutputStream _fos = null;
    protected BufferedOutputStream _bos = null;
    protected ByteArrayOutputStream _baos = null;  // aka memory stream

    protected byte[] _memoryBuffer = null;

    public BinaryOutputStream(String Filename) throws FileNotFoundException {
        this(Filename, 0, UUID.randomUUID().toString());
    }

    public BinaryOutputStream(String Filename, int MemoryLimitMegabytes) throws FileNotFoundException {
        this(Filename, MemoryLimitMegabytes, UUID.randomUUID().toString());
    }

    public BinaryOutputStream(String Filename, int MemoryLimitMegabytes, String FingerPrint) throws FileNotFoundException {
        _filename = Filename;
        if (FingerPrint != null) {
            _fingerPrint = FingerPrint;
        } else {
            _fingerPrint = UUID.randomUUID().toString();
        }

        if (MemoryLimitMegabytes == -1) {        // Memory only operation
            _isFilestream = false;

        } else if (MemoryLimitMegabytes == 0) {  // File only operation
            _isFilestream = true;
        } else {                                 // Hybrid operation
            _monitorMemory = true;
            _memoryLimit = MemoryLimitMegabytes * 1048576;
        }

        // Open the correct output type
        if (_isFilestream) {
            _fos = new FileOutputStream(_filename);
            _bos = new BufferedOutputStream(_fos);
            _dos = new DataOutputStream(_bos);
        } else {
            _baos = new ByteArrayOutputStream();
            _dos = new DataOutputStream(_baos);
        }
        _isOpen = true;
    }

    @Override
    public void close() throws IOException {
        try {
            if (_dos != null) {
                _dos.flush();
                _dos.close();
            }
        } catch (IOException ex) {
        }
        try {
            if (_bos != null) {
                _bos.flush();
                _bos.close();
            }
        } catch (IOException ex) {
        }
        try {
            if (_fos != null) {
                _fos.close();
            }
        } catch (IOException ex) {
        }
        _isOpen = false;
    }

    public void writeFinalHeader(byte[] b) throws IOException {
        close();
        if (_isFilestream) {
            try (RandomAccessFile raf = new RandomAccessFile(_filename, "rw")) {
                raf.write(b);
                raf.close();
            } 
        } else {
            _memoryBuffer = _baos.toByteArray();
            System.arraycopy(b, 0, _memoryBuffer, 0, b.length);
        }
    }
    
    public boolean IsFilestream() {
        return _isFilestream;
    }

    public long getPosition() {
       return _dos.size();
    }
    
    public byte[] getBuffer() {
        return _memoryBuffer;
    }

    public void write(byte[] b) throws IOException {
        memoryMonitor();
        _dos.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        memoryMonitor();
        _dos.write(b, off, len);
    }

    public void write(int b) throws IOException {
        memoryMonitor();
        _dos.write(b);
    }

    public void writeBoolean(boolean v) throws IOException {
        memoryMonitor();
        _dos.writeBoolean(v);
    }

    public void writeByte(int v) throws IOException {
        memoryMonitor();
        _dos.writeByte(v);
    }

    public void writeBytes(String s) throws IOException {
        memoryMonitor();
        _dos.writeBytes(s);
    }

    public void writeChar(int v) throws IOException {
        memoryMonitor();
        _dos.writeChar(v);
    }

    public void writeChars(String s) throws IOException {
        memoryMonitor();
        _dos.writeChars(s);
    }

    public void writeDouble(double v) throws IOException {
        memoryMonitor();
        _dos.writeDouble(v);
    }

    public void writeFloat(float v) throws IOException {
        memoryMonitor();
        _dos.writeFloat(v);
    }

    public void writeInt(int v) throws IOException {
        memoryMonitor();
        _dos.writeInt(v);
    }

    public void writeLong(long v) throws IOException {
        memoryMonitor();
        _dos.writeLong(v);
    }

    public void writeShort(int v) throws IOException {
        memoryMonitor();
        _dos.writeShort(v);
    }

    public void writeUTF(String str) throws IOException {
        memoryMonitor();
        _dos.writeUTF(str);
    }

    protected void memoryMonitor() throws IOException {
        if (!_monitorMemory) {
            return;
        }

        if (_dos.size() > _memoryLimit) {
            try {
                this.close();
                byte[] aMemoryContents = _baos.toByteArray();
                _fos = new FileOutputStream(_filename);
                _bos = new BufferedOutputStream(_fos);
                _dos = new DataOutputStream(_bos);

                _dos.write(aMemoryContents);
                _isFilestream = true;
                _monitorMemory = false;
                _isOpen = true;
            } catch (IOException ex) {
                throw new IOException("Stream switching error: " + ex.getMessage(), ex);
            }
        }
    }

}
